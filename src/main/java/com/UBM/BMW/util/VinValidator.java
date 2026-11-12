package com.UBM.BMW.util;

import java.util.Locale;
import java.time.Year;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/*
vin check which works for both usdm and edm bmws
na vins must carry a valid check digit in pos. 9 of the vin, and the model year
in pos. 10 of the vin.
european vins only have to be 17 valid characters, so no check digit, and pos. 10 isn't
guaranteed for it to be the year.
so if there's a failed check digit, it'll raise a warning, and why the model uear is
only decoded when the vin looks like a na vin.
 */

public final class VinValidator
{
    private static final String TRANSLITERATION_KEYS ="ABCDEFGHJKLMNPRSTUVWXYZ";
    private static final int[] TRANSLITERATION_VALUES = {1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 7, 9, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int [] WEIGHTS = {8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final String YEAR_CODES = "ABCDEFGHJKLMNPRSTVWXY123456789";
    private static final Map <String, String> Manufactures = Map.of(
            "WBA", "BMW AG (Germany)",
            "WBS", "BMW M GmbH (Germany)",
            "WBY", "BMW i (Germany)",
            "WMW", "MINI",
            "5UX", "BMW Manufacturing (Spartanburg, US)",
            "5YM", "BMW M (Spartanburg, US)",
            "4US", "BMW Manufacturing (Spartanburg, US)",
            "3MW", "BMW de Mexico (San Luis Potosi)",
            "LBV", "BMW Brilliance (China)"
    );
    private enum Market
    {
        NORTH_AMERICA,
        OUTSIDE_NORTH_AMERICA
    }

    public record Inspect(String vin, String wmi, String manufacturer, Market likelyMarket, Integer modelYear, String shortVin, List<String> notes)
    {
        public boolean isBMWGroup()
        {
            return manufacturer != null;
        }
    }

    private VinValidator()
    {

    }
    public static String normalize(String vin)
    {
        if (vin == null)
        {
            return null;
        }
        String v =vin.trim().toUpperCase(Locale.ROOT);
        return v.isEmpty() ? null : v;
    }
    /*
        check digit (pos. 9) is requiredin every car built in us or canada, doesn't matter where it was
        manufactured (germany, us, canada, mexico, etc.), and a WBA and 5UX both have to pass. cars built for
        other markets like a e65/66 745d for example, wouldn't have to pass the check digit, so it would gloss over that. focusing
        on WBA, WBS, WBY, 5UX, 5YM, and 4US, and making sure they're all valid.
     */
    public static boolean isFormed(String vin)
    {
        return vin != null && vin.matches("[A-HJ-NPR-Z0-9]{17}");
    }
    public static boolean validCheck(String vin)
    {
        if(!isFormed(vin))
        {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++)
        {
            char c = vin.charAt(i);
            int value = Character.isDigit(c) ? c - '0' : TRANSLITERATION_VALUES[TRANSLITERATION_KEYS.indexOf(c)];
            sum += value * WEIGHTS[i];
        }
        int remainder = sum%11;
        char expected = remainder == 10 ? 'X' : (char) ('0' + remainder);
        return vin.charAt(8) == expected;
    }

    //inspects vin, throws illegalargumentexception if not valid
    public static Inspect inspect(String vin)
    {
        if (!isFormed(vin))
        {
            throw new IllegalArgumentException("VIN must be 17 characters, must not contain I, O, or Q");
        }
        List<String> notes = new ArrayList<>();
        String wmi = vin.substring(0,3);
        String manufacturer = Manufactures.get(wmi);
        if(manufacturer == null)
        {
            notes.add("'"+wmi+"' isn't a BMW group manufacturer code that is recognized by this site.");
        }
        Market market;
        Integer modelYear = null;
        if(wmi.equals("LBV"))
        {
            market = Market.OUTSIDE_NORTH_AMERICA;
            notes.add("Built in China for the Chinese market");
        }
        else if(validCheck(vin))
        {
            market = Market.NORTH_AMERICA;
            modelYear = northAmericanMY(vin);
        }
        else
        {
            market = Market.OUTSIDE_NORTH_AMERICA;
            notes.add("Check digit doesn't match. That's normal for a European-market (EDM) or other import; " + "for a US or Canada car it means a typo.");
            notes.add("Model year can't be read from a non-North-American VIN; take it from the registration papers.");
        }
        return new Inspect(vin, wmi, manufacturer, market, modelYear, vin.substring(10), List.copyOf(notes));
    }
    static Integer northAmericanMY(String vin)
    {
        int index= YEAR_CODES.indexOf(vin.charAt(9));
        if(index < 0)
        {
            return null;
        }
        int year = 1980 + index;
        if(Character.isLetter(vin.charAt(6)))
        {
            year += 30;
        }
        if(year > Year.now().getValue()+1)
        {
            year -=30;
        }
        return year;
    }
}
