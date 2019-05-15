package org.miniproject.safesweeper;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestUnitTest {

    private static MainActivity testMode;

    @BeforeClass
    public static void setupClass(){
        testMode = new MainActivity();
    }

    @Test
    public void testConvertToDouble(){
        String validDoubleStr = "55.5";
        String invalidDoubleStr = "a3";

        assertEquals(55.5, testMode.convertToDouble(validDoubleStr), 0.1);
        assertEquals(0.0, testMode.convertToDouble(invalidDoubleStr), 0.1);
    }

    @Test
    public void testConvertLocation(){
        String regex = ("\\d+°\\s\\d+′\\s\\d+(\\.[0-9]+)?″\\s[WSEN]");
        String location = "45.2345";
        assertTrue(testMode.convertLocation(location).matches(regex));
    }

}
