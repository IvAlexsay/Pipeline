package comC;

import java.util.ArrayList;
import java.util.List;

public class Grammar {
    final static String splitter = "=";
    public final ArrayList<String> paramWords = new ArrayList<>();

    public Grammar(List<String> words){
        paramWords.addAll(words);
    }
    boolean getParamByWord(String word){
        for(String param : paramWords){
            if(param.equals(word)){
                return true;
            }
        }
        return false;
    }
    int GetCountWords(){
        return paramWords.size();
    }
}