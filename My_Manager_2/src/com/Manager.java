package com;

import com.java_polytech.pipeline_interfaces.IExecutor;
import com.java_polytech.pipeline_interfaces.IReader;
import com.java_polytech.pipeline_interfaces.IWriter;
import com.java_polytech.pipeline_interfaces.RC;
import comC.Config;
import comC.Grammar;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Manager {
    public enum paramWords{//Keywords for parameters
        InputFile,
        OutputFile,
        ReaderClassName,
        WriterClassName,
        ExecutorClassName,
        ReaderConfigFile,
        WriterConfigFile,
        ExecutorConfigFile
    }
    final static String executorSplitter = ",";
    IReader reader;
    IWriter writer;
    ArrayList<IExecutor> executor = new ArrayList<>();
    PipelineParams params;
    InputStream inputStream;
    OutputStream outputStream;
    Grammar gram;
    Logger logger;
    String[] executorsConfigFiles;

    class PipelineParams {
        public final String inputFile;
        public final String outputFile;
        public final String readerClassName;
        public final String writerClassName;
        public final String executorClassName;
        public final String readerConfigFile;
        public final String writerConfigFile;
        public final String executorConfigFile;

        PipelineParams(String inputFile, String outputFile, String readerClassName, String writerClassName,
                       String executorClassName, String readerConfigFile, String writerConfigFile, String executorConfigFile) {
            this.inputFile = inputFile;
            this.outputFile = outputFile;
            this.readerClassName = readerClassName;
            this.writerClassName = writerClassName;
            this.executorClassName = executorClassName;
            this.readerConfigFile = readerConfigFile;
            this.writerConfigFile = writerConfigFile;
            this.executorConfigFile = executorConfigFile;
        }
    }
    Manager(Logger logger){
        this.logger = logger;
    }

    RC SetConfig(String configName){
        gram = new Grammar(Arrays.asList(paramWords.InputFile.toString(),
                paramWords.OutputFile.toString(),
                paramWords.ReaderClassName.toString(),
                paramWords.WriterClassName.toString(),
                paramWords.ExecutorClassName.toString(),
                paramWords.ReaderConfigFile.toString(),
                paramWords.WriterConfigFile.toString(),
                paramWords.ExecutorConfigFile.toString()));
        Config config = new Config();
        RC rc = config.ReadConfig(configName, gram);
        if (!rc.isSuccess())
            return rc;
        params = new PipelineParams(config.params.get(paramWords.InputFile.toString()),
                config.params.get(paramWords.OutputFile.toString()),
                config.params.get(paramWords.ReaderClassName.toString()),
                config.params.get(paramWords.WriterClassName.toString()),
                config.params.get(paramWords.ExecutorClassName.toString()),
                config.params.get(paramWords.ReaderConfigFile.toString()),
                config.params.get(paramWords.WriterConfigFile.toString()),
                config.params.get(paramWords.ExecutorConfigFile.toString()));
        return RC.RC_SUCCESS;
    }
    RC CheckValidParams(){
        if (!Files.exists(Paths.get(params.inputFile))) {
            return RC.RC_MANAGER_CONFIG_SEMANTIC_ERROR;
        }
        if (!Files.exists(Paths.get(params.readerConfigFile))) {
            return RC.RC_MANAGER_CONFIG_SEMANTIC_ERROR;
        }
        executorsConfigFiles = params.executorConfigFile.split(executorSplitter);
        if(executorsConfigFiles.length != executor.size()){
            return RC.RC_MANAGER_CONFIG_SEMANTIC_ERROR;
        }
        for (String filename:
             executorsConfigFiles) {
            if (!Files.exists(Paths.get(filename))) {
                return RC.RC_MANAGER_CONFIG_SEMANTIC_ERROR;
            }
        }
        if (!Files.exists(Paths.get(params.writerConfigFile))) {
            return RC.RC_MANAGER_CONFIG_SEMANTIC_ERROR;
        }
        return RC.RC_SUCCESS;
    }

    RC OpenFiles() {
        try {
            inputStream = new FileInputStream(params.inputFile);
        } catch (FileNotFoundException ex) {
            return RC.RC_MANAGER_INVALID_INPUT_FILE;
        }
        try {
            outputStream = new FileOutputStream(params.outputFile);
        } catch (IOException ex) {
            return RC.RC_MANAGER_INVALID_OUTPUT_FILE;
        }
        return RC.RC_SUCCESS;
    }

    RC CloseFiles(){
        try {
            inputStream.close();
        } catch (IOException ex) {
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Invalid close input file");
        }
        try {
            outputStream.close();
        } catch (IOException ex) {
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Invalid close output file");
        }
        return RC.RC_SUCCESS;
    }

    RC GetClassesByName(){
        Class<?> tmp;
        try {
            tmp = Class.forName(params.readerClassName);
            if (IReader.class.isAssignableFrom(tmp))
                reader = (IReader) tmp.getDeclaredConstructor().newInstance();
            else
                return RC.RC_MANAGER_INVALID_READER_CLASS;
        }
        catch (Exception e) {
            return RC.RC_MANAGER_INVALID_READER_CLASS;
        }
        try {
            tmp = Class.forName(params.writerClassName);
            if (IWriter.class.isAssignableFrom(tmp))
                writer = (IWriter) tmp.getDeclaredConstructor().newInstance();
            else
                return RC.RC_MANAGER_INVALID_WRITER_CLASS;
        }
        catch (Exception e) {
            return RC.RC_MANAGER_INVALID_WRITER_CLASS;
        }
        String[] executorsNames = params.executorClassName.split(executorSplitter);
        for (String name:
                executorsNames) {
            try {
                tmp = Class.forName(name);
                if (IExecutor.class.isAssignableFrom(tmp))
                    executor.add((IExecutor)tmp.getDeclaredConstructor().newInstance());
                else
                    return RC.RC_MANAGER_INVALID_EXECUTOR_CLASS;
            }
            catch (Exception e) {
                return RC.RC_MANAGER_INVALID_EXECUTOR_CLASS;
            }
        }


        return RC.RC_SUCCESS;
    }

    RC Run(String configName){
        RC rc;
        rc = SetConfig(configName);
        if (!rc.isSuccess()){
            return rc;
        }
        if (!(rc = GetClassesByName()).isSuccess())
            return rc;
        if (!(rc = CheckValidParams()).isSuccess())
            return rc;
        if (!(rc = OpenFiles()).isSuccess())
            return rc;
        logger.log(Level.INFO, "Manager's config was successfully parsed. Start to built pipeline...");
        if (!(rc = reader.setConsumer(executor.get(0))).isSuccess())
            return rc;
        for(int i = 1; i < executor.size();i++){
            if (!(rc = executor.get(i-1).setConsumer(executor.get(i))).isSuccess())
                return rc;
        }
        if (!(rc = executor.get(executor.size()-1).setConsumer(writer)).isSuccess())
            return rc;
        if (!(rc = reader.setConfig(params.readerConfigFile)).isSuccess())
            return rc;
        for(int i = 0; i < executor.size();i++) {
            if (!(rc = executor.get(i).setConfig(executorsConfigFiles[i])).isSuccess())
                return rc;
        }
        if (!(rc = writer.setConfig(params.writerConfigFile)).isSuccess())
            return rc;
        if (!(rc = reader.setInputStream(inputStream)).isSuccess())
            return rc;
        if (!(rc = writer.setOutputStream(outputStream)).isSuccess())
            return rc;
        logger.log(Level.INFO,"Pipeline was successfully built. Start pipeline...");
        rc = reader.run();
        if(!rc.isSuccess())
            return rc;
        logger.log(Level.INFO,"Pipeline was successfully finished. Close files...");
        rc = CloseFiles();
        return rc;
    }


}
