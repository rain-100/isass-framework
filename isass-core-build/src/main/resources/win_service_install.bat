@echo off
chcp 65001
set path=%cd%

JavaService.exe -install @project.artifactId@ "%JAVA_HOME%/jre/bin/server/jvm.dll" -Djava.class.path="%JAVA_HOME%/jre/lib/tools.jar;%path%/@project.artifactId@-exec.jar" -Xms6G -Xmx6G -Xmn3G -XX:SurvivorRatio=8 -XX:InitialSurvivorRatio=8 -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=512M -Dfile.encoding=UTF-8 -start org.springframework.boot.loader.JarLauncher -stop org.springframework.boot.loader.JarLauncher -method systemExit -out "%path%/logs/out.log" -err "%path%/logs/err.log" -current "%path%" -auto -overwrite -description @service-name-cn@
@echo.

pause