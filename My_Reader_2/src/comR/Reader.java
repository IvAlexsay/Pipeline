package comR;

import com.java_polytech.pipeline_interfaces.*;
import comC.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.java_polytech.pipeline_interfaces.TYPE.*;

public class Reader implements IReader {
    public enum paramWords {//Keywords for parameters
        MaxBufferSize
    }
    InputStream inputStream;
    IConsumer consumer;
    PipelineParams params;
    Grammar gram;
    byte[] buffer;
    int bufSize;
    boolean chEnd;//Check of end file( 0 - OK | 1 - End)

    private final TYPE[] readerTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};

    static final int availableSize = 1000000;

    class PipelineParams {
        public final String maxBufferSize;
        PipelineParams(String maxBufferSize) {
            this.maxBufferSize = maxBufferSize;
        }
    }

    RC CheckValidParams(){
        int size = Integer.parseInt(params.maxBufferSize);
        if (size < availableSize && size > 0) {
            bufSize = size;
            return RC.RC_SUCCESS;
        }
        return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
    }

    boolean EndFile(){
        return chEnd;
    }

    void Read() throws IOException {
        if(inputStream.available() < bufSize){
            buffer = new byte[inputStream.available()];
            chEnd = true;
        }else{
            buffer = new byte[bufSize];
        }
        if(inputStream.read(buffer) < 0){
            buffer = null;
        }
    }

    @Override
    public RC setInputStream(InputStream inputStream) {
        if(inputStream != null){
            this.inputStream = inputStream;
        }
        else{
            return RC.RC_READER_FAILED_TO_READ;
        }
        return RC.RC_SUCCESS;
    }

    @Override
    public RC setConfig(String s) {
        gram = new Grammar(Arrays.asList(paramWords.MaxBufferSize.toString()));
        Config config = new Config();
        RC rc = config.ReadConfig(s, gram);
        if(!rc.isSuccess())
            return rc;
        params = new PipelineParams(config.params.get(paramWords.MaxBufferSize.toString()));
        rc = CheckValidParams();
        if(!rc.isSuccess())
            return rc;
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

    @Override
    public RC run() {
        while(!EndFile()) {
            try {
                Read();
            } catch (IOException e) {
                return RC.RC_READER_FAILED_TO_READ;
            }
            RC rc = consumer.consume();
            if(!rc.isSuccess())
                return rc;
        }
        buffer = null;
        RC rc = consumer.consume();
        if(!rc.isSuccess())
            return rc;
        return RC.RC_SUCCESS;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return readerTypes;
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
    class CharArrayMediator implements IMediator {
        public Object getData() {
            if (buffer.length == 0) {
                return null;
            }
            char[] chars = new String(buffer, StandardCharsets.UTF_8).toCharArray();
            return chars;
        }
    }
    class IntArrayMediator implements IMediator {
        public Object getData() {
            if (buffer.length == 0) {
                return null;
            }
            IntBuffer buf = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
            int[] array = new int[buf.remaining()];
            buf.get(array);
            return array;
        }
    }

    @Override
    public IMediator getMediator(TYPE type) {
        if (type == BYTE_ARRAY)
            return new ByteArrayMediator();
        else if (type == CHAR_ARRAY)
            return new CharArrayMediator();
        else if (type == INT_ARRAY)
            return new IntArrayMediator();
        else
            return null;
    }
}
