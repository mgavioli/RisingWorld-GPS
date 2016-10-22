/****************************
	G P S  -  A Java plug-in for Rising World.

	GpsGUI.java - The GUI Panel to interact with the GUI engine

	Created by : Maurizio M. Gavioli 2016-10-20

	(C) Maurizio M. Gavioli (a.k.a. Miwarre), 2016
	Licensed under the Creative Commons by-sa 3.0 license (see http://creativecommons.org/licenses/by-sa/3.0/ for details)

*****************************/

package com.vistamaresoft.gps;

import net.risingworld.api.gui.GuiElement;
import net.risingworld.api.gui.GuiLabel;
import net.risingworld.api.gui.GuiPanel;
import net.risingworld.api.gui.PivotPosition;
import net.risingworld.api.objects.Player;

public class GpsGUI extends GuiPanel
{
	// CONSTANTS
	//
	// The default position (relative to screen size) and size (absolute) of the GUI panel
	// Position centred in the screen
	// Size has been determined by trials and errors to fit all the required text.
	private static final	float	PANEL_XPOS		= 0.5f;
//	private static final	float	PANEL_YPOS		= 0.3f;
	private static final	int		PANEL_WIDTH		= 600;
	private static final	int		PANEL_HEIGHT	= 150;
	private static final	int		PANEL_COLOUR	= 0x20202080;
	private static final	int		BORDER_COLOUR	= 0x000000FF;
	private static final	int		BORDER_THICKNESS= 2;
	private static final	int		BORDER			= 6;
	// BUTTON & TEXT PROPERTIES
	private static final	int		BUTTON_COLOUR	= 0x0000C0FF;
	private static final	int		BUTTON_INACTIVE	= 0x404040FF;
	private static final	int		BUTTON_SIZE		= 18;
	private static final	int		TEXT_SIZE		= 18;
	// The title label
	private static final	int		TITLE_SIZE		= 24;
	private static final	int		TITLE_XPOS		= BORDER;
	private static final	int		TITLE_YPOS		= (PANEL_HEIGHT - BORDER);
	// The ON/OFF button
	private static final	int		ONOFF_XPOS		= (PANEL_WIDTH / 2);
	private static final	int		ONOFF_YPOS		= (TITLE_YPOS - TITLE_SIZE / 2);
	// the CLOSE button
	private static final	int		CLOSE_XPOS		= (PANEL_WIDTH *11 / 12);
	private static final	int		CLOSE_YPOS		= ONOFF_YPOS;
	// The HOME label and various button
	private static final	int		PREV_XPOS		= BORDER + 25;
	private static final	int		PREV_YPOS		= (ONOFF_YPOS - BORDER - BUTTON_SIZE*2);
	private static final	int		WPTXT_XPOS		= (PANEL_WIDTH / 2);
	private static final	int		WPTXT_YPOS		= PREV_YPOS;
	private static final	int		NEXT_XPOS		= PANEL_WIDTH - 25 - BORDER;
	private static final	int		NEXT_YPOS		= PREV_YPOS;
	private static final	int		GOTO_XPOS		= (PANEL_WIDTH * 1 / 12);
	private static final	int		GOTO_YPOS		= (WPTXT_YPOS - BORDER - BUTTON_SIZE * 2);
	private static final	int		HOMESET_XPOS	= (PANEL_WIDTH * 3 / 12);
	private static final	int		HOMESET_YPOS	= GOTO_YPOS;
	private static final	int		HOMESHOW_XPOS	= (PANEL_WIDTH * 5 / 12);
	private static final	int		HOMESHOW_YPOS	= GOTO_YPOS;
	private static final	int		WPSET_XPOS		= (PANEL_WIDTH * 7 / 12);
	private static final	int		WPSET_YPOS		= GOTO_YPOS;
	private static final	int		WPSHOW_XPOS		= (PANEL_WIDTH * 9 / 12);
	private static final	int		WPSHOW_YPOS		= GOTO_YPOS;
	private static final	int		WPHIDE_XPOS		= (PANEL_WIDTH *11 / 12);
	private static final	int		WPHIDE_YPOS		= GOTO_YPOS;

	// FIELDS
	//
	private GuiLabel		buttonClose;	// the "CLOSE" button
	private GuiLabel		buttonGoto;		// the "go to way point" button
	private	GuiLabel		buttHomeSet;	// the "set home" button
	private	GuiLabel		buttHomeShow;	// the "show/hide home" button
	private	GuiLabel		buttonNext;		// the "next wp" button
	private	GuiLabel		buttonOnOff;	// the button to turn GPS on/off
	private	GuiLabel		buttonPrev;		// the "prev wp" button
	private	GuiLabel		buttWpHide;		// the "hide waypoint" button
	private	GuiLabel		buttWpSet;		// the "set way point" button
	private	GuiLabel		buttWpShow;		// the "show waypoint" button
	private int				currWp;			// the current wp displayed in the GUI
	private GuiLabel		labelWp;		// the label with current way point data

	public GpsGUI(Player player, float infoYPos)
	{
		// create a panel centred in the screen right above or below the info window,
		// depending the latter is below or above the middle of the screen
		super(PANEL_XPOS, infoYPos + (infoYPos < 0.5f ? 0.1f : - 0.25f), true,
				PANEL_WIDTH, PANEL_HEIGHT, false);
		setPivot(PivotPosition.Center);
		setBorderColor(BORDER_COLOUR);
		setBorderThickness(BORDER_THICKNESS, false);
		setColor(PANEL_COLOUR);
		setVisible(false);
		currWp				= 0;
		// the title
		GuiLabel	title	= new GuiLabel("G P S", TITLE_XPOS, TITLE_YPOS, false);
		title.setPivot(PivotPosition.TopLeft);
//		title.setFontColor(0x00FFFFFF);
		title.setFontSize(TITLE_SIZE);
		addChild(title);
		player.addGuiElement(title);
		// the main ON / OFF button
		buttonOnOff			= new GuiLabel(ONOFF_XPOS, ONOFF_YPOS, false);
		buttonOnOff.setPivot(PivotPosition.Center);
		buttonOnOff.setColor(BUTTON_COLOUR);
		buttonOnOff.setFontSize(BUTTON_SIZE);
		buttonOnOff.setClickable(true);
		addChild(buttonOnOff);
		player.addGuiElement(buttonOnOff);
		// PREV & NEXT buttons
		buttonPrev	= new GuiLabel(PREV_XPOS, PREV_YPOS, false);
		buttonPrev.setPivot(PivotPosition.Center);
		buttonPrev.setColor(BUTTON_COLOUR);
		buttonPrev.setFontSize(BUTTON_SIZE);
		buttonPrev.setClickable(true);
		buttonPrev.setText(Msgs.button_prev);
		addChild(buttonPrev);
		player.addGuiElement(buttonPrev);
		buttonNext	= new GuiLabel(NEXT_XPOS, NEXT_YPOS, false);
		buttonNext.setPivot(PivotPosition.Center);
		buttonNext.setColor(BUTTON_COLOUR);
		buttonNext.setFontSize(BUTTON_SIZE);
		buttonNext.setClickable(true);
		buttonNext.setText(Msgs.button_next);
		addChild(buttonNext);
		player.addGuiElement(buttonNext);
		// Label for way point data
		labelWp		= new GuiLabel(WPTXT_XPOS, WPTXT_YPOS, false);
		labelWp.setPivot(PivotPosition.Center);
		labelWp.setFontSize(TEXT_SIZE);
		addChild(labelWp);
		player.addGuiElement(labelWp);
		// GOTO button
		buttonGoto	= new GuiLabel(GOTO_XPOS, GOTO_YPOS, false);
		buttonGoto.setPivot(PivotPosition.Center);
		buttonGoto.setColor(BUTTON_COLOUR);
		buttonGoto.setFontSize(BUTTON_SIZE);
		buttonGoto.setClickable(true);
		buttonGoto.setText(Msgs.button_goto);
		addChild(buttonGoto);
		player.addGuiElement(buttonGoto);
		// SET HOME button
		buttHomeSet	= new GuiLabel(HOMESET_XPOS, HOMESET_YPOS, false);
		buttHomeSet.setPivot(PivotPosition.Center);
		buttHomeSet.setColor(BUTTON_COLOUR);
		buttHomeSet.setFontSize(BUTTON_SIZE);
		buttHomeSet.setClickable(true);
		buttHomeSet.setText(Msgs.button_homeset);
		addChild(buttHomeSet);
		player.addGuiElement(buttHomeSet);
		// SHOW/HIDE HOME button
		buttHomeShow	= new GuiLabel(HOMESHOW_XPOS, HOMESHOW_YPOS, false);
		buttHomeShow.setPivot(PivotPosition.Center);
		buttHomeShow.setColor(BUTTON_COLOUR);
		buttHomeShow.setFontSize(BUTTON_SIZE);
		buttHomeShow.setClickable(true);
		buttHomeShow.setText(Msgs.button_homeshow);
		addChild(buttHomeShow);
		player.addGuiElement(buttHomeShow);
		// SET WP button
		buttWpSet	= new GuiLabel(WPSET_XPOS, WPSET_YPOS, false);
		buttWpSet.setPivot(PivotPosition.Center);
		buttWpSet.setColor(BUTTON_COLOUR);
		buttWpSet.setFontSize(BUTTON_SIZE);
		buttWpSet.setClickable(true);
		buttWpSet.setText(Msgs.button_wpset);
		addChild(buttWpSet);
		player.addGuiElement(buttWpSet);
		// SHOW WP button
		buttWpShow	= new GuiLabel(WPSHOW_XPOS, WPSHOW_YPOS, false);
		buttWpShow.setPivot(PivotPosition.Center);
		buttWpShow.setColor(BUTTON_COLOUR);
		buttWpShow.setFontSize(BUTTON_SIZE);
		buttWpShow.setClickable(true);
		buttWpShow.setText(Msgs.button_wpshow);
		addChild(buttWpShow);
		player.addGuiElement(buttWpShow);
		// HIDE WP button
		buttWpHide	= new GuiLabel(WPHIDE_XPOS, WPHIDE_YPOS, false);
		buttWpHide.setPivot(PivotPosition.Center);
		buttWpHide.setColor(BUTTON_COLOUR);
		buttWpHide.setFontSize(BUTTON_SIZE);
		buttWpHide.setClickable(true);
		buttWpHide.setText(Msgs.button_wphide);
		addChild(buttWpHide);
		player.addGuiElement(buttWpHide);
		// CLOSE button
		buttonClose	= new GuiLabel(CLOSE_XPOS, CLOSE_YPOS, false);
		buttonClose.setPivot(PivotPosition.Center);
		buttonClose.setColor(BUTTON_COLOUR);
		buttonClose.setFontSize(BUTTON_SIZE);
		buttonClose.setClickable(true);
		buttonClose.setText(Msgs.button_close);
		addChild(buttonClose);
		player.addGuiElement(buttonClose);
	}

	/**
		Shows the GUI panel for the Player @a player.
		The panel is filled with appropriate texts and made visible.
		Attaching it to the proper player should be managed by the caller.
		@param player	the player to which this panel refers to
	*/
	public void show(Player player)
	{
		player.setMouseCursorVisible(true);
		updateControls(player);
		setVisible(true);
	}

	/**
		Process a mouse click on the child GUI element @a element. 

		@param	element	the element which has been clicked
		@param	player	the player this panel refers to
	*/
	public void click(GuiElement element, Player player)
	{
		Waypoint[]	waypoints		= (Waypoint[])player.getAttribute(Gps.key_gpsWpList);
		boolean		isCurrWpDefined	= false;
		if (waypoints != null)
			isCurrWpDefined			= waypoints[currWp] != null;

		// check for the clicked 'control'
		if (element == buttonOnOff)				// TURN GPS ON/OFF
		{
			Gps.setGPSShow(player, !(boolean)player.getAttribute(Gps.key_gpsShow));
		}
		else if (element == buttonPrev)			// PREV WP
		{
			currWp--;
			if (currWp < 0)				// wrap to last wp
				currWp = Gps.maxWp;
		}
		else if (element == buttonNext)			// NEXT WP
		{
			currWp++;
			if (currWp > Gps.maxWp)		// wrap to first wp
				currWp = 0;
		}
		else if (element == buttonGoto)			// GOTO current wp
		{
			if (isCurrWpDefined)
				Gps.teleportToWp(player, currWp);
		}
		else if (element == buttHomeSet)		// SET HOME to current position
			Db.setHome(player);
		else if (element == buttHomeShow)		// SHOW HOME ON/OFF
			Gps.setShowHome(player);
		else if (element == buttWpSet)			// SET WP to current position
			Gps.setWp(player, String.valueOf(currWp), null);
		else if (element == buttWpShow)			// SHOW current wp data
		{
			if (currWp != Gps.homeWp && isCurrWpDefined)
				Gps.setShowWp(player, currWp);
		}
		else if (element == buttWpHide)			// HIDE WP data
			Gps.setShowWp(player, 0);
		else if (element == buttonClose)		// CLOSE the 'dialogue box'
		{
			player.setMouseCursorVisible(false);
			setVisible(false);
		}
		else
			return;
		updateControls(player);
	}

	/**
		Updates the 'controls' of the 'dialogue box' to current GPS data status.
		
		Because of a bug (or a 'feature'?) in the current plug-in API, unexpected
		results are generated if the colour of the 'clickability' of a GuiElement
		are changed while the element is added to a player GUI. For this reason,
		each element is removed before the being changed and re-added after.
		Changing the text seems do not suffer form this bug.
		
		@param player	the player this GUI belongs to.
	*/
	private void	updateControls(Player player)
	{
		// set global ON/OFF depending on GPS being OFF/ON
		buttonOnOff.setText((boolean)player.getAttribute(Gps.key_gpsShow) ?
				Msgs.button_off : Msgs.button_on);

		// the following controls depends upon HOME and/or the current wp being
		// defined or not
		Waypoint[]	waypoints = (Waypoint[]) player.getAttribute(Gps.key_gpsWpList);
		Waypoint	wp;
		if (waypoints != null)
		{
			wp = waypoints[currWp];
			// wp text and GOTO button
			if (wp != null)
			{
				// set waypoint data text and GOTO button status
				String		txt;
				if ( (txt = wp.toString()) != null)
				{
					labelWp.setText(txt);				// if curr. wp is defined, show data
					buttonGoto.setColor(BUTTON_COLOUR);	// and enable GOTO button
					buttonGoto.setClickable(true);
				}
			}
			else
			{
				labelWp.setText("" + currWp + Msgs.txt_undefined);	// otherwise, show no data
				buttonGoto.setColor(BUTTON_INACTIVE);	// and disable GOTO button
				buttonGoto.setClickable(false);
			}
			// set HOME SET and HOME SHOW/HIDE buttons
			boolean	isDef	= (waypoints[0] != null);		// if HOME defined?
			boolean isShown	= (boolean)player.getAttribute(Gps.key_gpsHomeShow);
			// set HOME SHOW/HIDE text depending on home being currently shown or not
			buttHomeShow.setText(isShown ? Msgs.button_homehide : Msgs.button_homeshow);
			// enable/disable HOME SHOW/HIDE depending on home being defined or not
			buttHomeShow.setColor(isDef ? BUTTON_COLOUR : BUTTON_INACTIVE);
			buttHomeShow.setClickable(isDef ? true : false);
			// WP buttons
			isShown	= ((int)player.getAttribute(Gps.key_gpsWpShow) != 0);
			// enable/disable WP SET depending on the current wp being Home or not
			buttWpShow.setColor(currWp != 0 ? BUTTON_COLOUR : BUTTON_INACTIVE);
			buttWpShow.setClickable(currWp != 0 ? true : false);
			// enable/disable WP SHOW depending on curr. wp being defined or not
			buttWpShow.setColor(wp != null ? BUTTON_COLOUR : BUTTON_INACTIVE);
			buttWpShow.setClickable(wp != null ? true : false);
			// enable/disable WP HIDE depending on some wp being shown or not
			buttWpHide.setColor(isShown ? BUTTON_COLOUR : BUTTON_INACTIVE);
			buttWpHide.setClickable(isShown ? true : false);
		}
	}

}
