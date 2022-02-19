package com;

import com.java_polytech.pipeline_interfaces.RC;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Main {
    public static void main(String[] args)  {
        Logger logger = Logger.getLogger("Pipeline");
        if(args[0] != null) {
            Manager manager = new Manager(logger);//Get config
            RC rc = manager.Run(args[0]);
            if(!rc.isSuccess()){
                logger.log(Level.SEVERE, rc.info);
            }
            else{
                logger.log(Level.SEVERE, RC.RC_SUCCESS.info);
            }
        }else{
            logger.log(Level.SEVERE,RC.RC_MANAGER_INVALID_ARGUMENT.info);
        }
    }
}
