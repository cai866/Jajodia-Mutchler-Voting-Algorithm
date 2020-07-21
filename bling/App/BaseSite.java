package bling.App;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static bling.Util.util.println;

public class BaseSite {
    public Logger logger;
    public static int loggerNum = 1;

    {
        Logger baseLogger = Logger.getLogger("");
        baseLogger.getHandlers()[0].setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getLoggerName() + " - "  +
                        new SimpleDateFormat("HH:mm:ss:SSSS").format(new Date()) +
                        " - " + Thread.currentThread().getName() +
                        ": " + record.getMessage() +"\n";
            }
        });
    }

    public BaseSite(){
        logger = Logger.getLogger(this.getClass().getSimpleName()+"-" + loggerNum);
        loggerNum ++;
    }
}
