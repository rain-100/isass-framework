// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NocodeInitializationRunnerTest {

    @TempDir
    Path directory;

    @Test
    void routesParsedWorkbookRowsToTheOwningRemoteService() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("sample");
            sheet.createRow(0).createCell(0).setCellValue("id");
            sheet.createRow(1).createCell(0).setCellValue("主键");
            sheet.createRow(2).createCell(0).setCellValue("2081264465596485633");
            try (var output = Files.newOutputStream(directory.resolve("remote.xlsx"))) {
                workbook.write(output);
            }
        }
        var data = new NocodeInitializationDataService(List.of());
        var remote = mock(NocodeInitializationRemoteClient.class);
        var definition = mock(ServiceDefinition.class);
        when(definition.hasNocodeOperations()).thenReturn(true);
        doReturn(NocodeInitializationDataServiceTest.SampleEntrypoint.class).when(definition).serviceInterface();
        when(definition.resourceName()).thenReturn("sample");
        when(definition.serviceName()).thenReturn("sample-service");
        var definitions = mock(ServiceDefinitionRegistry.class);
        when(definitions.all()).thenReturn(List.of(definition));
        var document = Map.of("sample", List.of(Map.of("id", "2081264465596485633")));
        when(remote.importData("sample-service", document))
                .thenReturn(new NocodeInitializationDataService.ImportResult(1, 1, 0, Map.of()));

        var importer = new NocodeInitializationImporter(data, remote, definitions);
        new NocodeInitializationRunner(data, importer, properties()).runner()
                .run(new DefaultApplicationArguments());

        verify(remote).importData("sample-service", document);
    }

    @Test
    void alwaysContinuesWithTheNextFileAfterAParseFailure() throws Exception {
        Files.writeString(directory.resolve("a.xlsx"), "invalid workbook");
        Files.writeString(directory.resolve("b.json"), "{}");
        var data = mock(NocodeInitializationDataService.class);
        var remote = mock(NocodeInitializationRemoteClient.class);
        when(data.readDocument(any(), eq("a.xlsx"), any())).thenThrow(new IllegalArgumentException("bad workbook"));
        when(data.readDocument(any(), eq("b.json"), any())).thenReturn(Map.of());
        var definitions = mock(ServiceDefinitionRegistry.class);
        when(definitions.all()).thenReturn(List.of());
        var properties = properties();
        var importer = new NocodeInitializationImporter(data, remote, definitions);
        new NocodeInitializationRunner(data, importer, properties).runner()
                .run(new DefaultApplicationArguments());

        verify(data).readDocument(any(), eq("b.json"), any());
        verifyNoInteractions(remote);
    }

    @Test
    void discoversAllFormatsInStableOrderAndIgnoresExcelLockFiles() throws Exception {
        Files.writeString(directory.resolve("b.xlsx"), "parsed by reader");
        Files.writeString(directory.resolve("a.json"), "parsed by reader");
        Files.writeString(directory.resolve("c--sample.csv"), "parsed by reader");
        Files.writeString(directory.resolve("~$b.xlsx"), "lock");
        Files.writeString(directory.resolve("notes.txt"), "ignored");
        Files.createDirectory(directory.resolve("nested"));
        var data = mock(NocodeInitializationDataService.class);
        var remote = mock(NocodeInitializationRemoteClient.class);
        var definitions = mock(ServiceDefinitionRegistry.class);
        when(definitions.all()).thenReturn(List.of());
        var document = Map.of("sample", List.<Map<String, Object>>of(Map.of("id", "1")));
        when(data.readDocument(any(), anyString(), any())).thenReturn(document);
        when(data.localServiceName("sample")).thenReturn("sample-service");
        when(data.hasLocalService("sample-service")).thenReturn(true);
        when(data.importData("sample-service", document))
                .thenReturn(new NocodeInitializationDataService.ImportResult(1, 1, 0, Map.of()));
        var properties = properties();
        var importer = new NocodeInitializationImporter(data, remote, definitions);
        new NocodeInitializationRunner(data, importer, properties).runner()
                .run(new DefaultApplicationArguments());

        var ordered = inOrder(data);
        ordered.verify(data).readDocument(any(), eq("a.json"), any());
        ordered.verify(data).readDocument(any(), eq("b.xlsx"), any());
        ordered.verify(data).readDocument(any(), eq("c--sample.csv"), any());
        verifyNoInteractions(remote);
    }

    @Test
    void malformedWorkbookDoesNotBlockTheNextValidFile() throws Exception {
        Files.writeString(directory.resolve("invalid.xlsx"), "not a workbook");
        Files.writeString(directory.resolve("next.json"), "{}");
        var data = spy(new NocodeInitializationDataService(List.of()));
        var remote = mock(NocodeInitializationRemoteClient.class);
        var definitions = mock(ServiceDefinitionRegistry.class);
        when(definitions.all()).thenReturn(List.of());
        var importer = new NocodeInitializationImporter(data, remote, definitions);
        var runner = new NocodeInitializationRunner(data, importer, properties()).runner();
        runner.run(new DefaultApplicationArguments());
        verify(data).readDocument(any(), eq("next.json"), any());
        verifyNoInteractions(remote);
    }

    private NocodeInitializationProperties properties() {
        var properties = new NocodeInitializationProperties();
        properties.setLocation(directory.toUri() + "**/*");
        return properties;
    }
}
