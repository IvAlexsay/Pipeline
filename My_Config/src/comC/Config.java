package comC;

import com.java_polytech.pipeline_interfaces.RC;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Config {
    public Map<String, String> params= new HashMap<>();

    public RC ReadConfig(String filename, Grammar gram){
            try {
                BufferedReader reader = new BufferedReader(new FileReader(filename));
                String line;
                while ((line = reader.readLine()) != null) {//Read lines and check grammar
                    String[] paramString = line.split(gram.splitter);
                    if (paramString.length != 2) {
                        continue;
                        //return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;
                    } else {
                        paramString[0] = paramString[0].replaceAll("\\s", "");
                        paramString[1] = paramString[1].replaceAll("\\s", "");
                        if (gram.getParamByWord(paramString[0]) == true) {
                            params.put(paramString[0], paramString[1]);
                        }
                    }
                }
                if (gram.GetCountWords() != params.size()) {
                    return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;
                }
            }
            catch (IOException ex){
                return RC.RC_MANAGER_CONFIG_FILE_ERROR;
            }
            return RC.RC_SUCCESS;
    }

}
