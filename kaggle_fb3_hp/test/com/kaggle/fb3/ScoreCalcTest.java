package com.kaggle.fb3;

import static org.junit.Assert.*;

import org.junit.Test;

public class ScoreCalcTest {

	@Test
	public void test() {
		double f1Score = ScoreCalc.calcF1("unsigned signed", "signed unsigned");
		System.out.println("F1 Score :: "+ f1Score);
		
		fail("Not yet implemented");
	}

}
