package vn.dorastudio.doraantispoof.util;

import java.nio.charset.StandardCharsets;

public final class BrandDecoder {

    private BrandDecoder() {}

    public static String decode(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        try {
            int[] position = {0};
            int length = readVarInt(data, position);

            if (length < 0 || length > data.length - position[0]) {
                return new String(data, StandardCharsets.UTF_8);
            }

            return new String(
                    data,
                    position[0],
                    length,
                    StandardCharsets.UTF_8
            );
        } catch (Exception exception) {
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private static int readVarInt(byte[] data, int[] position) {
        int result = 0;
        int shift = 0;

        while (position[0] < data.length && shift < 35) {
            byte current = data[position[0]++];
            result |= (current & 0x7F) << shift;

            if ((current & 0x80) == 0) {
                return result;
            }

            shift += 7;
        }

        throw new IllegalArgumentException("VarInt không hợp lệ");
    }
}
