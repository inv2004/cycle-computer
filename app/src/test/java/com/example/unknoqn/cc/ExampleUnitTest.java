package com.example.unknoqn.cc;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.example.unknoqn.cc.calc.CCCalcAutoInt;
import com.example.unknoqn.cc.calc.CCCalcAvgPwr;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void calcAverage() throws Exception {
        CCCalcAvgPwr calc = new CCCalcAvgPwr(null);
        calc.start(10);
        calc.calc0(20, 100);
        calc.calc0(30, 100);
        assertEquals(100, calc.calc0(50, 100));
        calc.stop();
        calc.start(10);
        calc.calc0(20, 100);
        calc.calc0(30, 200);
        assertEquals(150, calc.calc0(40, 150));
        calc.calc0(41, 100);
        calc.calc0(42, 200);
        calc.calc0(43, 100);
        calc.calc0(44, 200);
        calc.calc0(45, 100);
        assertEquals(150, calc.calc0(46, 200));
    }

    @Test
    public void calcAutoInt() throws Exception {
        CCCalcAutoInt calc = new CCCalcAutoInt(null);
        int r = calc.calc0_arr(1000,
                new long[]{1,2,3,4,5,6,7,8,9,10},
                new int[]{200,200,200,200,200,200,200,200,200,200});
        assertEquals(-10, r);
        r = calc.calc0_arr(1000,
                new long[]{11,12,13,14,15,16,17,18,19,20},
                new int[]{400,400,400,400,400,400,400,400,400,400});
        assertEquals(-9, r);
//        assertEquals(1, calc.calc0(100*210, 400));
    }
}
