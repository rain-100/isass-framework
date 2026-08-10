// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Files;
import java.nio.file.Path;
import vip.isass.framework.common.exception.UnifiedException;
import vip.isass.framework.common.exception.code.StatusMessageEnum;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Paths;
import java.util.Iterator;

@Slf4j
public class FileUtil {

    public static String getContentType(File file) {
        Assert.notNull(file);
        //利用nio提供的类判断文件ContentType
        String contentType = null;
        try {
            contentType = Files.probeContentType(file.toPath());
        } catch (IOException e) {
            // do nothing
        }
        //若失败则调用另一个方法进行判断
        if (contentType == null) {
            try {
                contentType = Files.probeContentType(file.toPath());
            } catch (IOException ignored) {
                // ignore
            }
            if (contentType == null) {
                String fileName = file.getName();
                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else {
                    contentType = "application/octet-stream";
                }
            }
        }
        return contentType;
    }

    public static boolean isImage(File file) {
        boolean isImage = false;
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
            isImage = iter.hasNext();
        } catch (IOException e) {
            log.error("文件[{}]是否图片判断失败", file.getName());
        }
        return isImage;
    }

    public static File getFileIgnoreCase(Path dirPath, String fileName) {
        Path filePath = dirPath.resolve(fileName);
        if (Files.exists(filePath)) {
            return filePath.toFile();
        }

        try (DirectoryStream<Path> paths = Files.newDirectoryStream(dirPath)) {
            for (Path loopPath : paths) {
                if (loopPath.getFileName().toString().equalsIgnoreCase(fileName)
                        && !Files.isDirectory(loopPath)) {
                    return loopPath.toFile();
                }
            }
        } catch (IOException e) {
            throw new UnifiedException(StatusMessageEnum.IO_ERROR);
        }

        return null;
    }

    public static Path getDesktopPath() {
        String userHomeDir = System.getProperty("user.home");
        if (StrUtil.isBlank(userHomeDir)) {
            return null;
        }
        Path desktopPath = Paths.get(userHomeDir, "Desktop");
        return Files.exists(desktopPath) ? desktopPath : null;
    }

}
