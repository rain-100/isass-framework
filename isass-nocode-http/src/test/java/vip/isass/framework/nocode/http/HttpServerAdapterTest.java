package vip.isass.framework.nocode.http;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.HttpMethod;
import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ParameterContract;
import vip.isass.framework.nocode.contract.ParameterSource;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.service.IService;
import vip.isass.framework.nocode.stream.FileStream;
import vip.isass.framework.nocode.stream.FileNotFoundException;

import java.io.InputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HttpServerAdapterTest {

    public interface CustomApi {
        String findAvailableIcons(Long tenantId);
    }

    public interface CriteriaApi {
        String firstCriteriaConditionValueType(TestIconCriteria criteria);
    }

    public interface StreamApi {
        String upload(InputStream file);

        FileStream download();
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
        IService service = mock(IService.class,
                org.mockito.Mockito.withSettings().extraInterfaces(CustomApi.class));
        when(service.service()).thenReturn("attachment-service");
        when(service.entity()).thenReturn("icon");
        when(((CustomApi) service).findAvailableIcons(9L)).thenReturn("icons");

        OperationContract operation = new OperationContract(
                "findAvailableIcons", HttpMethod.GET, "/available/{tenantId}",
                501, true,
                List.of(new ParameterContract(
                        "tenantId", Long.class.getName(), ParameterSource.PATH,
                        true, "租户 ID")),
                String.class.getName(), "查询可用图标");
        ServiceContract contract = new ServiceContract(
                "attachment-service", "icon", CustomApi.class.getName(),
                "example.Icon", "example.IconCriteria", List.of(operation));
        HttpServerAdapter adapter = new HttpServerAdapter(
                new ContractRegistry(List.of(contract)),
                new ServiceRegistry(List.of(service)),
                new ObjectMapper());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(adapter).build();

        mvc.perform(get("/attachment-service/icon/available/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("icons"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void queryParametersBindToCriteriaDefaultIdSetter() throws Exception {
        IService service = mock(IService.class,
                org.mockito.Mockito.withSettings().extraInterfaces(CriteriaApi.class));
        when(service.service()).thenReturn("attachment-service");
        when(service.entity()).thenReturn("icon");
        when(((CriteriaApi) service).firstCriteriaConditionValueType(any(TestIconCriteria.class)))
                .thenAnswer(invocation -> {
                    TestIconCriteria criteria = invocation.getArgument(0, TestIconCriteria.class);
                    if (criteria.getWhereConditions().isEmpty()) {
                        return "empty";
                    }
                    return criteria.getWhereConditions().getFirst().getValue().getClass().getName();
                });

        OperationContract operation = new OperationContract(
                "firstCriteriaConditionValueType", HttpMethod.GET, "/criteria",
                301, true,
                List.of(new ParameterContract(
                        "criteria", TestIconCriteria.class.getName(), ParameterSource.QUERY,
                        false, "查询条件")),
                Integer.class.getName(), "按条件查询");
        ServiceContract contract = new ServiceContract(
                "attachment-service", "icon", CriteriaApi.class.getName(),
                TestIcon.class.getName(), TestIconCriteria.class.getName(), List.of(operation));
        HttpServerAdapter adapter = new HttpServerAdapter(
                new ContractRegistry(List.of(contract)),
                new ServiceRegistry(List.of(service)),
                new ObjectMapper());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(adapter).build();

        mvc.perform(get("/attachment-service/icon/criteria?id=9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(Long.class.getName()));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void multipartInputStreamAndFileStreamUseRawStreams() throws Exception {
        IService service = mock(IService.class,
                org.mockito.Mockito.withSettings().extraInterfaces(StreamApi.class));
        when(service.service()).thenReturn("attachment-service");
        when(service.entity()).thenReturn("attachment");
        when(((StreamApi) service).upload(any(InputStream.class))).thenAnswer(invocation ->
                new String(invocation.getArgument(0, InputStream.class).readAllBytes()));
        when(((StreamApi) service).download()).thenReturn(new FileStream(
                "test.txt", "text/plain", 4L, true,
                output -> output.write("data".getBytes())));
        ServiceContract contract = new ServiceContract(
                "attachment-service", "attachment", StreamApi.class.getName(),
                "example.Attachment", "example.AttachmentCriteria", List.of(
                new OperationContract("upload", HttpMethod.POST, "/upload", 101, false,
                        List.of(new ParameterContract("file", InputStream.class.getName(),
                                ParameterSource.BODY, false, "上传文件")),
                        String.class.getName(), "上传"),
                new OperationContract("download", HttpMethod.GET, "/download", 301, true,
                        List.of(), FileStream.class.getName(), "下载")
        ));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new HttpServerAdapter(
                new ContractRegistry(List.of(contract)), new ServiceRegistry(List.of(service)),
                new ObjectMapper())).build();

        mvc.perform(multipart("/attachment-service/attachment/upload")
                        .file("file", "stream-content".getBytes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("stream-content"));
        mvc.perform(get("/attachment-service/attachment/download"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string("data"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void fileOperationReturnsEmptyNotFoundInsteadOfResp() throws Exception {
        IService service = mock(IService.class,
                org.mockito.Mockito.withSettings().extraInterfaces(StreamApi.class));
        when(service.service()).thenReturn("attachment-service");
        when(service.entity()).thenReturn("attachment");
        when(((StreamApi) service).download()).thenThrow(new FileNotFoundException("附件不存在"));
        OperationContract operation = new OperationContract(
                "download", HttpMethod.GET, "/download", 301, true,
                List.of(), FileStream.class.getName(), "下载");
        ServiceContract contract = new ServiceContract(
                "attachment-service", "attachment", StreamApi.class.getName(),
                "example.Attachment", "example.AttachmentCriteria", List.of(operation));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new HttpServerAdapter(
                new ContractRegistry(List.of(contract)), new ServiceRegistry(List.of(service)),
                new ObjectMapper())).build();

        mvc.perform(get("/attachment-service/attachment/download"))
                .andExpect(status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(""));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void fileOperationReturnsEmptyServerErrorInsteadOfResp() throws Exception {
        IService service = mock(IService.class,
                org.mockito.Mockito.withSettings().extraInterfaces(StreamApi.class));
        when(service.service()).thenReturn("attachment-service");
        when(service.entity()).thenReturn("attachment");
        when(((StreamApi) service).download()).thenThrow(new IllegalStateException("storage unavailable"));
        OperationContract operation = new OperationContract(
                "download", HttpMethod.GET, "/download", 301, true,
                List.of(), FileStream.class.getName(), "下载");
        ServiceContract contract = new ServiceContract(
                "attachment-service", "attachment", StreamApi.class.getName(),
                "example.Attachment", "example.AttachmentCriteria", List.of(operation));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new HttpServerAdapter(
                new ContractRegistry(List.of(contract)), new ServiceRegistry(List.of(service)),
                new ObjectMapper())).build();

        mvc.perform(get("/attachment-service/attachment/download"))
                .andExpect(status().isInternalServerError())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(""));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void multipartFormFieldsBindToDtoAndDefaultFileNameToOriginalName() throws Exception {
        IService service = mock(IService.class,
                org.mockito.Mockito.withSettings().extraInterfaces(MultipartApi.class));
        when(service.service()).thenReturn("attachment-service");
        when(service.entity()).thenReturn("attachment");
        when(((MultipartApi) service).upload(any(UploadRequest.class), any(InputStream.class)))
                .thenAnswer(invocation -> {
                    UploadRequest request = invocation.getArgument(0, UploadRequest.class);
                    String content = new String(invocation.getArgument(1, InputStream.class).readAllBytes());
                    return request.getBizType() + ":" + request.getFileName() + ":" + content;
                });
        OperationContract operation = new OperationContract(
                "upload", HttpMethod.POST, "/upload", 101, false,
                List.of(
                        new ParameterContract("request", UploadRequest.class.getName(),
                                ParameterSource.BODY, true, "上传参数"),
                        new ParameterContract("file", InputStream.class.getName(),
                                ParameterSource.BODY, true, "上传文件")
                ), String.class.getName(), "上传");
        ServiceContract contract = new ServiceContract(
                "attachment-service", "attachment", MultipartApi.class.getName(),
                "example.Attachment", "example.AttachmentCriteria", List.of(operation));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new HttpServerAdapter(
                new ContractRegistry(List.of(contract)), new ServiceRegistry(List.of(service)),
                new ObjectMapper())).build();

        mvc.perform(multipart("/attachment-service/attachment/upload")
                        .file(new MockMultipartFile("file", "original.txt", "text/plain",
                                "stream-content".getBytes()))
                        .param("bizType", "contract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("contract:original.txt:stream-content"));
    }

    public static class TestIcon implements IIdEntity<Long, TestIcon> {

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
            extends FullTypeCriteria<TestIcon, TestIconCriteria>
            implements IIdCriteria<Long, TestIcon, TestIconCriteria> {
    }
}
