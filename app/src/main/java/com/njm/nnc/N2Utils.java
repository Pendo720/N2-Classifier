package com.njm.nnc;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiresApi(api = Build.VERSION_CODES.N)
public class N2Utils {


    public static String stringifyList(List<Double> doubles){
        final String[] stoReturn = {""};
        doubles.forEach(d-> stoReturn[0] += String.format(Locale.UK,"% 6.5f,", d));
        stoReturn[0] = stoReturn[0].substring(0, stoReturn[0].length()-1);
        return stoReturn[0];
    }

    public static List<Double> int2BitDoubles(int target, int bits){
        return Stream.of(String.format("%0" + bits +"d", Integer.valueOf(Integer.toBinaryString(target))).split(""))
                .filter(s->!s.isEmpty())
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }

    public static double GetRandom() { return new Random().nextDouble(); }
}
