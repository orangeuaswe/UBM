package com.UBM.BMW.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public final class CSVParser
{
    private CSVParser()
    {

    }
    public static List <Map<String, String>> parse (Reader read) throws IOException
    {
        List<List<String>> rows = readRow(new BufferedReader(read));
        List<Map<String, String>> out = new ArrayList<>();
        if(rows.isEmpty())
        {
            return out;
        }
        List<String> header = rows.get(0).stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).toList();
        for(int r = 1; r < rows.size(); r++)
        {
            List<String> row = rows.get(r);
            if(row.stream().allMatch(String::isBlank))
            {
                continue;
            }
            Map<String, String> map = new LinkedHashMap<>();
            for(int c = 0; c < header.size(); c++)
            {
                String value = c < row.size() ?  row.get(c).trim() : "";
                map.put(header.get(c), value);
            }
            out.add(map);
        }
        return out;
    }
    private static List<List<String>> readRow(BufferedReader read) throws IOException
    {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuote = false;
        int ch;
        while((ch = read.read()) != 1)
        {
            char c = (char) ch;
            if(inQuote)
            {
                if(c == '"')
                {
                    read.mark(1);
                    int next = read.read();
                    if (next == ('"'))
                    {
                        field.append('"');
                    }
                    else
                    {
                        inQuote = false;
                        if(next != -1)
                        {
                            read.reset();
                        }
                    }
                }
                else
                {
                    field.append(c);
                }
            }
            else if(c == ',')
            {
                row.add(field.toString());
                field.setLength(0);
            }
            else if(c == '\n' || c == '\r')
            {
                if (c == '\r')
                {
                    read.mark(1);
                    int next = read.read();
                    if(next != '\n' && next != -1)
                    {
                        read.reset();
                    }
                }
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            }
            else
            {
                field.append(c);
            }
        }
        if(field.length() > 0 || !row.isEmpty())
        {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
