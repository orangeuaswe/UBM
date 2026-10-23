package com.UBM.BMW.util;
import java.util.Locale;
public final class ModelNames
{
    private ModelNames()
    {

    }
    public static String normalize(String model)
    {
        if (model == null)
        {
            return null;
        }
        String string = model.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
        if(string.isEmpty())
        {
            return null;
        }
        string = string.replace("xdrive", "").replace("sdrive", "");
        if(string.matches("\\d{3}[lcx]i"))
        {
            string = string.substring(0,3)+"i";
        }
        return string.isEmpty() ? null : string;
    }
}