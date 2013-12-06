package com.kaggle.fb3;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class CleanDataTest {

	@Test
	public void test() {
		Assert.assertEquals("How to check if an uploaded file is an image without mime type", CleanData.cleanData("How to check if an uploaded file is an image without mime type?"));
	}

}
