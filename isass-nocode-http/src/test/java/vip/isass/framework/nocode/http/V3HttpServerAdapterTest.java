package vip.isass.framework.nocode.http;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.v3.V3ServiceRegistry;
import vip.isass.framework.nocode.v3.criteria.field.IV3IdCriteria;
import vip.isass.framework.nocode.v3.criteria.impl.type.V3FullTypeCriteria;
import vip.isass.framework.nocode.v3.entity.IV3IdEntity;
import vip.isass.framework.nocode.v3.contract.V3ContractRegistry;
import vip.isass.framework.nocode.v3.contract.V3HttpMethod;
import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ParameterContract;
import vip.isass.framework.nocode.v3.contract.V3ParameterSource;
import vip.isass.framework.nocode.v3.contract.V3ServiceContract;
import vip.isass.framework.nocode.v3.service.IV3Service;
import vip.isass.framework.nocode.v3.stream.V3FileStream;
import vip.isass.framework.nocode.v3.stream.V3FileNotFoundException;

import java.io.InputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class V3HttpServerAdapterTest {

    public interface CustomApi {
        String findAvailableIcons(Long tenantId);
    }

    public interface CriteriaApi {
        String firstCriteriaConditionValueType(TestIconCriteria criteria);
    }

    public interface StreamApi {
        String upload(InputStream file);

        V3FileStream download();
    }

    public interface MultipartApi {
        String upload(UploadRequest request, InputStream file);
    }

    public static class UploadRequest {
        private String bizType;
        private String fileName;

        public String getBizType() {
            return bizType;
        }

        public void setBizType(String bizType) {
            this.bizType = bizType;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void oneAdapterInvokesCustomMethodForEntity() throws Exception {
        IV3Service service = mock(IV3Service.class,
                org.mockito.Mockito.withSettings().extraInterfaces(CustomApi.class));
        when(service.serviceName()).thenReturn("attachment-service");
        when(service.entityName()).thenReturn("icon");
        when(((CustomApi) service).findAvailableIcons(9L)).thenReturn("icons");

        V3OperationContract operation = new V3OperationContract(
                "findAvailableIcons", V3HttpMethod.GET, "/available/{tenantId}",
                501, true,
                List.of(new V3ParameterContract(
                        "tenantId", Long.class.getName(), V3ParameterSource.PATH,
                        true, "租户 ID")),
                String.class.getName(), "查询可用图标");
        V3ServiceContract contract = new V3ServiceContract(
                "attachment-service", "icon", CustomApi.class.getName(),
                "example.Icon", "example.IconCriteria", List.of(operation));
        V3HttpServerAdapter adapter = new V3HttpServerAdapter(
                new V3ContractRegistry(List.of(contract)),
                new V3ServiceRegistry(List.of(service)),
                new ObjectMapper());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(adapter).build();

        mvc.perform(get("/attachment-service/icon/v3/available/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("icons"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void queryParametersBindToCriteriaDefaultIdSetter() throws Exception {
        IV3Service service = mock(IV3Service.class,
                org.mockito.Mockito.withSettings().extraInterfaces(CriteriaApi.class));
        when(service.serviceName()).thenReturn("attachment-service");
        when(service.entityName()).thenReturn("icon");
        when(((CriteriaApi) service).firstCriteriaConditionValueType(any(TestIconCriteria.class)))
                .thenAnswer(invocation -> {
                    TestIconCriteria criteria = invocation.getArgument(0, TestIconCriteria.class);
                    if (criteria.getWhereConditions().isEmpty()) {
                        return "empty";
                    }
                    return criteria.getWhereConditions().getFirst().getValue().getClass().getName();
                });

        V3OperationContract operation = new V3OperationContract(
                "firstCriteriaConditionValueType", V3HttpMethod.GET, "/criteria",
                301, true,
                List.of(new V3ParameterContract(
                        "criteria", TestIconCriteria.class.getName(), V3ParameterSource.QUERY,
                        false, "查询条件")),
                Integer.class.getName(), "按条件查询");
        V3ServiceContract contract = new V3ServiceContract(
                "attachment-service", "icon", CriteriaApi.class.getName(),
                TestIcon.class.getName(), TestIconCriteria.class.getName(), List.of(operation));
        V3HttpServerAdapter adapter = new V3HttpServerAdapter(
                new V3ContractRegistry(List.of(contract)),
                new V3ServiceRegistry(List.of(service)),
                new ObjectMapper());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(adapter).build();

        mvc.perform(get("/attachment-service/icon/v3/criteria?id=9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(Long.class.getName()));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void multipartInputStreamAndFileStreamUseRawStreams() throws Exception {
        IV3Service service = mock(IV3Service.class,
                org.mockito.Mockito.withSettings().extraInterfaces(StreamApi.class));
        when(service.serviceName()).thenReturn("attachment-service");
        when(service.entityName()).thenReturn("attachment");
        when(((StreamApi) service).upload(any(InputStream.class))).thenAnswer(invocation ->
                new String(invocation.getArgument(0, InputStream.class).readAllBytes()));
        when(((StreamApi) service).download()).thenReturn(new V3FileStream(
                "test.txt", "text/plain", 4L, true,
                output -> output.write("data".getBytes())));
        V3ServiceContract contract = new V3ServiceContract(
                "attachment-service", "attachment", StreamApi.class.getName(),
                "example.Attachment", "example.AttachmentCriteria", List.of(
                new V3OperationContract("upload", V3HttpMethod.POST, "/upload", 101, false,
                        List.of(new V3ParameterContract("file", InputStream.class.getName(),
                                V3ParameterSource.BODY, false, "上传文件")),
                        String.class.getName(), "上传"),
                new V3OperationContract("download", V3HttpMethod.GET, "/download", 301, true,
                        List.of(), V3FileStream.class.getName(), "下载")
        ));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new V3HttpServerAdapter(
                new V3ContractRegistry(List.of(contract)), new V3ServiceRegistry(List.of(service)),
                new ObjectMapper())).build();

        mvc.perform(multipart("/attachment-service/attachment/v3/upload")
                        .file("file", "stream-content".getBytes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("stream-content"));
        mvc.perform(get("/attachment-service/attachment/v3/download"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string("data"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void fileOperationReturnsEmptyNotFoundInsteadOfResp() throws Exception {
        IV3Service service = mock(IV3Service.class,
                org.mockito.Mockito.withSettings().extraInterfaces(StreamApi.class));
        when(service.serviceName()).thenReturn("attachment-service");
        when(service.entityName()).thenReturn("attachment");
        when(((StreamApi) service).download()).thenThrow(new V3FileNotFoundException("附件不存在"));
        V3OperationContract operation = new V3OperationContract(
                "download", V3HttpMethod.GET, "/download", 301, true,
                List.of(), V3FileStream.class.getName(), "下载");
        V3ServiceContract contract = new V3ServiceContract(
                "attachment-service", "attachment", StreamApi.class.getName(),
                "example.Attachment", "example.AttachmentCriteria", List.of(operation));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new V3HttpServerAdapter(
                new V3ContractRegistry(List.of(contract)), new V3ServiceRegistry(List.of(service)),
                new ObjectMapper())).build();

        mvc.perform(get("/attachment-service/attachment/v3/download"))
                .andExpect(status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(""));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void fileOperationReturnsEmptyServerErrorInsteadOfResp() throws Exception {
        IV3Service service = mock(IV3Service.class,
                org.mockito.Mockito.withSettings().extraInterfaces(StreamApi.class));
        when(service.serviceName()).thenReturn("attachment-service");
        when(service.entityName()).thenReturn("attachment");
        when(((StreamApi) service).download()).thenThrow(new IllegalStateException("storage unavailable"));
        V3OperationContract operation = new V3OperationContract(
                "download", V3HttpMethod.GET, "/download", 301, true,
                List.of(), V3FileStream.class.getName(), "下载");
        V3ServiceContract contract = new V3ServiceContract(
                "attachment-service", "attachment", StreamApi.class.getName(),
                "example.Attachment", "example.AttachmentCriteria", List.of(operation));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new V3HttpServerAdapter(
                new V3ContractRegistry(List.of(contract)), new V3ServiceRegistry(List.of(service)),
                new ObjectMapper())).build();

        mvc.perform(get("/attachment-service/attachment/v3/download"))
                .andExpect(status().isInternalServerError())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(""));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void multipartFormFieldsBindToDtoAndDefaultFileNameToOriginalName() throws Exception {
        IV3Service service = mock(IV3Service.class,
                org.mockito.Mockito.withSettings().extraInterfaces(MultipartApi.class));
        when(service.serviceName()).thenReturn("attachment-service");
        when(service.entityName()).thenReturn("attachment");
        when(((MultipartApi) service).upload(any(UploadRequest.class), any(InputStream.class)))
                .thenAnswer(invocation -> {
                    UploadRequest request = invocation.getArgument(0, UploadRequest.class);
                    String content = new String(invocation.getArgument(1, InputStream.class).readAllBytes());
                    return request.getBizType() + ":" + request.getFileName() + ":" + content;
                });
        V3OperationContract operation = new V3OperationContract(
                "upload", V3HttpMethod.POST, "/upload", 101, false,
                List.of(
                        new V3ParameterContract("request", UploadRequest.class.getName(),
                                V3ParameterSource.BODY, true, "上传参数"),
                        new V3ParameterContract("file", InputStream.class.getName(),
                                V3ParameterSource.BODY, true, "上传文件")
                ), String.class.getName(), "上传");
        V3ServiceContract contract = new V3ServiceContract(
                "attachment-service", "attachment", MultipartApi.class.getName(),
                "example.Attachment", "example.AttachmentCriteria", List.of(operation));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new V3HttpServerAdapter(
                new V3ContractRegistry(List.of(contract)), new V3ServiceRegistry(List.of(service)),
                new ObjectMapper())).build();

        mvc.perform(multipart("/attachment-service/attachment/v3/upload")
                        .file(new MockMultipartFile("file", "original.txt", "text/plain",
                                "stream-content".getBytes()))
                        .param("bizType", "contract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("contract:original.txt:stream-content"));
    }

    public static class TestIcon implements IV3IdEntity<Long, TestIcon> {

        private Long id;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public TestIcon randomEntity() {
            return this;
        }
    }

    public static class TestIconCriteria
            extends V3FullTypeCriteria<TestIcon, TestIconCriteria>
            implements IV3IdCriteria<Long, TestIcon, TestIconCriteria> {
    }
}
