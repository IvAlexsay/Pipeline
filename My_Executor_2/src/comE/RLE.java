package comE;

import java.util.ArrayList;

public class RLE {
    enum chSim {notSimilar, similar}
    static ArrayList<Byte> undecodedBuf = new ArrayList<>();
    /**Function of coded data;
     * Coding system:
     * - More than 3 similar bytes in a row are coded in 3 bytes:
     * first byte = type of next bytes(similar);
     * second byte = count of coded bytes;
     * third byte = repeating byte
     * - Not similar bytes:
     *  first byte = type of next bytes(not similar);
     *  second byte = count of coded bytes;
     *  next bytes = coded bytes
     * */
    public static boolean СheckIntegrity(){
        if(undecodedBuf.isEmpty()){
            return true;
        }
        else{
            return false;
        }
    }

    public static byte[] Code(byte[] buffer) {
        if (buffer.length == 0) {
            return buffer;
        }
        byte curByte = buffer[0];
        int curCount = 1;
        int curCountSimilar = 1;
        int iterator = 0;
        chSim flag = chSim.notSimilar;//flag that says which symbols we count (0 - not similar; 1 - similar)
        int point = 0;//number of start not similar elements
        Byte[] list = new Byte[buffer.length + 4 + buffer.length/ 127 * 2 ];
        //ArrayList<Byte> list = new ArrayList<>();
        for (int i = 1; i <= buffer.length; i++) {
            byte b = i < buffer.length ? buffer[i] : 0;
            if (i == buffer.length || curCount == 127 || (b != curByte && flag == chSim.similar && curCount > 3) || (b == curByte && flag == chSim.notSimilar && curCountSimilar == 3 && curCountSimilar < curCount)) {
                if (flag == chSim.similar) {
                    list[iterator++] = ((byte) flag.ordinal());
                    list[iterator++] = ((byte) (curCount));
                    list[iterator++] = (curByte);
                    curCount = 1;
                    curCountSimilar = 1;
                    flag = chSim.notSimilar;
                    point = i;
                } else if (flag == chSim.notSimilar && (i == buffer.length || curCount == 127)) {
                    list[iterator++] = ((byte) flag.ordinal());
                    list[iterator++] = ((byte) (curCount));
                    for (int j = point; j < point + curCount; j++) {
                        list[iterator++] = (buffer[j]);
                    }
                    curCount = 1;
                    curCountSimilar = 1;
                    point = i;
                } else {
                    curCount -= curCountSimilar;
                    list[iterator++] = ((byte) flag.ordinal());
                    list[iterator++] = ((byte) (curCount));
                    for (int j = point; j < point + curCount; j++) {
                        list[iterator++] = (buffer[j]);
                    }
                    curCount = curCountSimilar + 1;
                    curCountSimilar++;
                    flag = chSim.similar;
                }
            } else if (b == curByte) {
                curCountSimilar++;
                if (flag != chSim.similar && curCountSimilar > 3) {
                    flag = chSim.similar;
                }
                curCount++;
            } else {
                if (flag != chSim.notSimilar) {
                    flag = chSim.notSimilar;
                    point = i - 1;
                }
                curCount++;
                curCountSimilar = 1;
            }
            curByte = b;
        }
        byte[] done = new byte[iterator];
        for (int i = 0; i < iterator; i++) {
            done[i] = list[i];
        }
        return done;
    }

    /**Function of decoded data;
     * First byte = type of next bytes(Similar|Not similar);
     * Second byte = count of next bytes of type;
     * Next byte/bytes = data;
     */
    public static byte[] Decode(byte[] buffer) {
        if (buffer.length == 0) {
            return buffer;
        }
        byte[] done;

        if(buffer.length <= 3 && undecodedBuf.size() == 0){
            for (int i = 0; i < buffer.length; i++) {
                undecodedBuf.add(buffer[i]);
            }
            done = new byte[0];
            return done;
        }
        ArrayList<Byte> unite = new ArrayList<>(undecodedBuf);
        undecodedBuf.clear();
        for (int i = 0; i < buffer.length;i++) {
            unite.add(buffer[i]);
        }
        Byte[] list = new Byte[unite.size() / 3 * 127 ];
        int iterator = 0;
        int unsize = 0;
        for (int i = 0; i < unite.size(); i++) {
            if (unite.get(i) == chSim.notSimilar.ordinal()) {
                if((i+1) ==  unite.size()){
                    undecodedBuf.add(unite.get(i));
                    break;
                }
                int curCount = unite.get(i + 1);
                i += 2;
                for (int j = 0; j < curCount; j++) {
                    if(i == unite.size()){
                        iterator -= j;
                        unsize = j + 2;
                        break;
                    }
                    list[iterator++] = unite.get(i);
                    i++;
                }
                if(i == unite.size()) {
                    for (int k = i - unsize; k < i; k++) {
                        undecodedBuf.add(unite.get(k));
                    }
                    break;
                }
                i -=1;
            }
            else {
                if ((i + 1) == unite.size()) {
                    undecodedBuf.add(unite.get(i));
                    break;
                }
                if ((i + 2) == unite.size()) {
                    undecodedBuf.add(unite.get(i));
                    undecodedBuf.add(unite.get(i + 1));
                    break;
                }
                for (int j = 0; j < unite.get(i + 1); j++) {
                    list[iterator++] = unite.get(i + 2);
                }
                i += 2;
            }
        }

        done = new byte[iterator];
        for (int i = 0; i < iterator; i++) {
            done[i] = list[i];
        }
        return done;
    }
}
