@echo off
echo Compiling...
javac -cp "src\main\java;lib\*" -d bin src\main\java\com\fincore\server\SimpleHttpServer.java src\main\java\com\fincore\server\HttpRequest.java src\main\java\com\fincore\server\HttpResponse.java src\main\java\com\fincore\server\StaticFileHandler.java src\main\java\com\fincore\handler\AuthHandler.java src\main\java\com\fincore\handler\DashboardHandler.java src\main\java\com\fincore\handler\TransactionHandler.java src\main\java\com\fincore\dao\UserDAO.java src\main\java\com\fincore\dao\TransactionDAO.java src\main\java\com\fincore\dao\NotificationDAO.java src\main\java\com\fincore\model\User.java src\main\java\com\fincore\model\Transaction.java src\main\java\com\fincore\model\Notification.java src\main\java\com\fincore\util\DBConnection.java

if %errorlevel% neq 0 (
    echo Compilation Failed!
    pause
    exit /b %errorlevel%
)

echo Starting Server...
java -cp "bin;lib\*" com.fincore.server.SimpleHttpServer
pause
