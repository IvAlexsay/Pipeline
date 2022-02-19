package comW;

import com.java_polytech.pipeline_interfaces.*;
import comC.Config;
import comC.Grammar;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class Writer implements IWriter {
    public enum paramWords{//Keywords for parameters
        BufferSize
    }
    BufferedOutputStream bufferOutput;
    PipelineParams params;
    Grammar gram;
    IProvider provider;
    IMediator data;
    TYPE workerType;
    byte[] buffer;
    int bufSize;
    static final int availableSize = 1000000;

    private final TYPE[] executorTypes = {TYPE.BYTE_ARRAY};

    class PipelineParams {
        public final String BufferSize;

        PipelineParams(String maxBufferSize) {
            this.BufferSize = maxBufferSize;
        }
    }

    RC CheckValidParams(){
        int size = Integer.parseInt(params.BufferSize);
        if (size < availableSize && size > 0) {
            bufSize = size;
            return RC.RC_SUCCESS;
        }
        return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
    }

    @Override
    public RC setOutputStream(OutputStream outputStream) {
        if (outputStream == null)
            return RC.RC_WRITER_FAILED_TO_WRITE;
        if(bufSize != 0)
            bufferOutput = new BufferedOutputStream(outputStream, bufSize);
        return RC.RC_SUCCESS;
    }

    @Override
    public RC setConfig(String s) {
        gram =new Grammar(Arrays.asList(paramWords.BufferSize.toString()));
        Config config = new Config();
        RC rc = config.ReadConfig(s, gram);
        if(!rc.isSuccess())
            return rc;
        params = new PipelineParams(config.params.get(paramWords.BufferSize.toString()));
        rc = CheckValidParams();
        if(!rc.isSuccess())
            return rc;
        return RC.RC_SUCCESS;
    }

    @Override
    public RC setProvider(IProvider iProvider) {
        provider = iProvider;
        RC rc = chooseType(provider.getOutputTypes());
        if(!rc.isSuccess()){
            return rc;
        }
        data = provider.getMediator(workerType);
        return RC.RC_SUCCESS;
    }

    public RC chooseType(TYPE[] types){
        for (TYPE here : executorTypes ) {
            for (TYPE there : types) {
                if(here == there){
                    workerType = here;
                    return RC.RC_SUCCESS;
                }
            }
        }
        return RC.RC_EXECUTOR_TYPES_INTERSECTION_EMPTY_ERROR;
    }

    @Override
    public RC consume() {
        buffer = (byte[]) data.getData();
        if (buffer == null) {
            try {
                bufferOutput.flush();
            }
            catch (IOException e) {
                return RC.RC_WRITER_FAILED_TO_WRITE;
            }
            return RC.RC_SUCCESS;
        }
        try {
            bufferOutput.write(buffer);
        }
        catch (IOException e) {
            return RC.RC_WRITER_FAILED_TO_WRITE;
        }
        return RC.RC_SUCCESS;
    }
}
