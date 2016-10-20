/****************************
	G P S  -  A Java plug-in for Rising World.

	Messages.java - The internationalisation support

	Created by : Maurizio M. Gavioli 2016-08-15

	(C) Maurizio M. Gavioli (a.k.a. Miwarre), 2016
	Licensed under the Creative Commons by-sa 3.0 license (see http://creativecommons.org/licenses/by-sa/3.0/ for details)

*****************************/

package com.vistamaresoft.gps;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/*
 * WORK IN PROGRESS
 * 
 * This feature is not completed yet!
 */

public class Messages {
	private static final String BUNDLE_NAME = "com.vistamaresoft.gps.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private Messages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
