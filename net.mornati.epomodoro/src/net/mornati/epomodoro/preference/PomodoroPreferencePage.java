package net.mornati.epomodoro.preference;

import net.mornati.epomodoro.Activator;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PomodoroPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String GROUP_NAME="GROUP_NAME";
	public static final String CLIENT_NAME="CLIENT_NAME";
	public static final String POMODORO_TIME="POMODORO_TIME";
	public static final String POMODORO_PAUSE="POMODORO_PAUSE";
	public static final String WORK_PAUSE_AUTO_SWITCH="WORK_PAUSE_AUTO_SWITCH";
	public static final String DISCARD_OWN_MESSAGE="DISCARD_OWN_MESSAGE";
	public static final String SHOW_TIMER_STATUS_BAR="SHOW_TIMER_STATUS_BAR";
	public static final String FORCE_IPV4="FORCE_IPV4";
	public static final String POMODORO_DESCRIPTION="POMODORO_DESCRIPTION";
	public static final String BIND_IP_ADDR="BIND_IP_ADDR";
	public static final String POMODORO_TIME_COLOR = "POMODORO_TIME_COLOR";
	public static final String POMODORO_PAUSE_COLOR = "POMODORO_PAUSE_COLOR";

	public PomodoroPreferencePage() {
		super(GRID);

	}

	public void createFieldEditors() {
		addField(new StringFieldEditor(GROUP_NAME, "Team Name:", getFieldEditorParent()));
		addField(new StringFieldEditor(CLIENT_NAME, "Your Name:", getFieldEditorParent()));
		addField(new IntegerFieldEditor(POMODORO_TIME, "Pomodoro Time (minutes):", getFieldEditorParent()));
		addField(new ColorFieldEditor(POMODORO_TIME_COLOR, "Pomodoro Time font color", getFieldEditorParent()));
		addField(new IntegerFieldEditor(POMODORO_PAUSE, "Pomodoro Pause (minutes):", getFieldEditorParent()));
		addField(new ColorFieldEditor(POMODORO_PAUSE_COLOR, "Pomodoro Pause font color", getFieldEditorParent()));
		addField(new BooleanFieldEditor(WORK_PAUSE_AUTO_SWITCH, "Auto start pause", getFieldEditorParent()));
		addField(new BooleanFieldEditor(DISCARD_OWN_MESSAGE, "Discard own message in team table", getFieldEditorParent()));
		addField(new BooleanFieldEditor(SHOW_TIMER_STATUS_BAR, "Show timer in status bar", getFieldEditorParent()));
		addField(new BooleanFieldEditor(FORCE_IPV4, "Force IPv4", getFieldEditorParent()));
		addField(new BooleanFieldEditor(POMODORO_DESCRIPTION, "Set a Pomodoro description", getFieldEditorParent()));
		addField(new StringFieldEditor(BIND_IP_ADDR, "Bind IP Address (optional):", getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("ePomodoro Plugin Preference Page");
	}
}
