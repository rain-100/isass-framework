@echo off

net stop @project.artifactId@
sc delete @project.artifactId@
@echo.

pause