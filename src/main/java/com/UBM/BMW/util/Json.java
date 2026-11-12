package com.UBM.BMW.util;

import java.util.Map;
public final class Json
{
    private Json()
    {

    }
    public static String write(Object value)
    {
        StringBuilder string = new StringBuilder();
        write(string, value);
        return string.toString();
    }
    private static void write (StringBuilder string, Object value)
    {
        if(value == null)
        {
            string.append("null");
        }
        else if(value instanceof Number || value instanceof Boolean)
        {
            string.append(value);
        }
        else if(value instanceof Map<?,?> map)
        {
            string.append('{');
            boolean first = true;
            for(Map.Entry<?,?> e : map.entrySet())
            {
                if(!first)
                {
                    string.append(',');
                }
                first = false;
                quote(string, String.valueOf(e.getKey()));
                string.append(':');
                write(string, e.getValue());
            }
            string.append('}');
        }
        else if(value instanceof Iterable<?> items)
        {
            string.append('[');
            boolean first = true;
            for(Object item : items)
            {
                if (!first)
                {
                    string.append(',');
                }
                first = false;
                write(string,item);
            }
        }
        else
        {
            quote(string, value instanceof Enum<?>e ? e.name() : value.toString());
        }
    }

    private static void quote (StringBuilder string, String s)
    {
        string.append('"');
        for(int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            switch (c)
            {
                case '"' -> string.append("\\\"");
                case '\\' -> string.append("\\\\");
                case '\n' -> string.append("\\n");
                case '\r' -> string.append("\\r");
                case '\t' -> string.append("\\t");
                default -> {
                    if (c < 0x20)
                    {
                        string.append(String.format("\\u%04x", (int) c));
                    }
                    else
                    {
                        string.append(c);
                    }
                }
                
            }
        }
        string.append('"');
    }

}
