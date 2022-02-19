package comE;

import com.java_polytech.pipeline_interfaces.*;
import comC.Config;
import comC.Grammar;

import java.util.Arrays;

import static com.java_polytech.pipeline_interfaces.TYPE.*;

public class Executor implements IExecutor {
    public enum paramWords {
        Mode
    }
    enum mode_t {
        CODE,
        DECODE
    }
    mode_t mode;
    PipelineParams params;
    IConsumer consumer;
    IProvider provider;
    IMediator data;
    Grammar gram;
    TYPE workerType;
    byte[] buffer;

    private final TYPE[] executorTypes = {TYPE.BYTE_ARRAY};

    class PipelineParams {
        public final String mode;
        PipelineParams(String maxBufferSize) {
            this.mode = maxBufferSize;
        }
    }

    RC CheckValidParams(){
        if (params.mode.equals(mode_t.CODE.toString())) {
            mode = mode_t.CODE;
            return RC.RC_SUCCESS;
        }
        if (params.mode.equals(mode_t.DECODE.toString())) {
            mode = mode_t.DECODE;
            return RC.RC_SUCCESS;
        }
        return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
    }

    @Override
    public RC setConfig(String s) {
        gram = new Grammar(Arrays.asList(paramWords.Mode.toString()));
        Config config = new Config();
        RC rc = config.ReadConfig(s, gram);
        if(!rc.isSuccess())
            return rc;
        params = new PipelineParams(config.params.get(paramWords.Mode.toString()));
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

    @Override
    public RC setConsumer(IConsumer iConsumer) {
        consumer = iConsumer;
        RC rc = iConsumer.setProvider(this);
        if(!rc.isSuccess()){
            return rc;
        }
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
        if(buffer == null){
            if(RLE.СheckIntegrity()){
                consumer.consume();
            }
            else{
                return RC.RC_EXECUTOR_CONFIG_FILE_ERROR;
            }
        }
        else {
            if (mode == mode_t.CODE) {
                buffer = RLE.Code(buffer);
            } else {
                buffer = RLE.Decode(buffer);
            }
            consumer.consume();
        }
        return RC.RC_SUCCESS;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return executorTypes;
    }

    class ByteArrayMediator implements IMediator {
        public Object getData() {
            if (buffer == null) {
                return null;
            }
            byte[] copy = new byte[buffer.length];
            System.arraycopy(buffer, 0, copy, 0, buffer.length);
            return copy;
        }
    }

    @Override
    public IMediator getMediator(TYPE type) {
        if (type == BYTE_ARRAY)
            return new ByteArrayMediator();
        else
            return null;
    }
}
