@echo off
echo 正在关闭可能占用文件的进程...
taskkill /F /IM node.exe /T
taskkill /F /IM electron.exe /T
timeout /t 2 /nobreak > nul

echo 删除 node_modules 目录...
if exist node_modules (
  rmdir /s /q node_modules
  if exist node_modules (
    echo 无法删除 node_modules 目录，请手动删除后重试
    pause
    exit /b 1
  )
)

echo 删除 package-lock.json...
if exist package-lock.json del /f package-lock.json

echo 清除 npm 缓存...
call npm cache clean --force

echo 重新安装依赖...
call npm install --no-fund --no-audit

echo 安装完成！
pause 