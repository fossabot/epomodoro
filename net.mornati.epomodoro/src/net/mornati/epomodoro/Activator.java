package net.mornati.epomodoro;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.mornati.epomodoro.communication.AbstractPomodoroMessage;
import net.mornati.epomodoro.communication.Communication;
import net.mornati.epomodoro.communication.TextMessage;
import net.mornati.epomodoro.communication.TimerMessage;
import net.mornati.epomodoro.preference.PomodoroPreferencePage;
import net.mornati.epomodoro.util.ConflictRule;
import net.mornati.epomodoro.util.Log;
import net.mornati.epomodoro.util.PomodoroTimer;
import net.mornati.epomodoro.util.UIUtil;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID="net.mornati.epomodoro"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private Communication communication;
	private ISchedulingRule jobRule;
	private PomodoroTimer timer;
	private boolean showDialog=false;
	private Timer scheduler=new Timer();
	private final SimpleDateFormat sdf=new SimpleDateFormat("mm : ss");
	private final List<Button> startButtons=new ArrayList<Button>();
	private final List<Label> counterLabels=new ArrayList<Label>();
	private final List<TimerMessage> receivedMessages=new ArrayList<TimerMessage>();
	private TableViewer viewer;
	private String taskDescription = "";

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin=this;
		IPreferenceStore preferenceStore=Activator.getDefault().getPreferenceStore();
		int preferenceTime=preferenceStore.getInt(PomodoroPreferencePage.POMODORO_TIME);
		createTimer(preferenceTime * 60 * 1000, PomodoroTimer.TYPE_WORK);
		initCommunication();
		sendTimerMessage();
		checkMessages();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin=null;
		super.stop(context);
		scheduler.cancel();
		scheduler=null;
		timer.interrupt();
		timer=null;
		communication.close();
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public void initCommunication() {
		Job job=new Job("ConnectToJGroups") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IPreferenceStore preferenceStore=getPreferenceStore();
				String groupName=preferenceStore.getString(PomodoroPreferencePage.GROUP_NAME);
				communication=Communication.getInstance();
				try {
					communication.connect(groupName);
				} catch (Exception e) {
					e.printStackTrace();
				}

				return Status.OK_STATUS;
			}

		};
		job.setUser(false);
		job.setRule(getRule());
		job.schedule();
	}

	private void sendTimerMessage() {
		TimerTask task=new TimerTask() {

			@Override
			public void run() {
				if (communication != null && communication.isConnected() && timer != null) {
					TimerMessage message=(TimerMessage) Communication.createMessage(TimerMessage.class);
					message.setTimer(timer.getFormatTime());
					message.setStatus(timer.getStatus());
					try {
						communication.sendMessage(message);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
		};

		scheduler.schedule(task, 1000, 5000);
	}

	public Communication getCommunication() {
		return communication;
	}

	public ISchedulingRule getRule() {
		if (jobRule == null) {
			jobRule=new ConflictRule();
		}
		return jobRule;
	}

	public PomodoroTimer getTimer() {
		return timer;
	}

	public PomodoroTimer createTimer(long totalTime, int type) {
		if (timer == null || timer.getTime() == 0) {
			timer=new PomodoroTimer(totalTime, type);
		}
		return timer;
	}

	public PomodoroTimer resetTimer(long totalTime, int type) {
		if (timer != null) {
			timer.interrupt();
			timer=new PomodoroTimer(totalTime, type);
		}
		taskDescription = "";
		return timer;
	}

	public void setShowDialog(boolean showDialog) {
		this.showDialog=showDialog;
	}

	public boolean isShowDialog() {
		return showDialog;
	}

	public Timer getScheduler() {
		return scheduler;
	}

	public void subscribeStartButton(Button button) {
		startButtons.add(button);
	}

	public void subscribeCounterLabel(Label label) {
		counterLabels.add(label);
	}

	public List<Button> getStartButtons() {
		return startButtons;
	}

	public void scheduleTimer(final int changeInterval) {
		final PomodoroTimer internalTimer;
		if (timer == null) {
			internalTimer=createTimer(timer.getConfigWorkTime(), PomodoroTimer.TYPE_WORK);
		} else {
			internalTimer=timer;
		}
		Display.getDefault().timerExec(changeInterval, new Runnable() {

			@Override
			public void run() {
				IPreferenceStore preferenceStore=getPreferenceStore();
				if (internalTimer != null) {
					for (Label countdown : counterLabels) {
						if (!countdown.isDisposed()) {
							if (timer.getType() == PomodoroTimer.TYPE_WORK) {
								countdown.setForeground(new Color(Display.getDefault(), StringConverter.asRGB(preferenceStore.getString(PomodoroPreferencePage.POMODORO_TIME_COLOR))));
							} else {
								countdown.setForeground(new Color(Display.getDefault(), StringConverter.asRGB(preferenceStore.getString(PomodoroPreferencePage.POMODORO_PAUSE_COLOR))));
							}
							//							countdown.setToolTipText(timer.getType() == PomodoroTimer.TYPE_PAUSE ? "Pause Timer" : "Work Timer");
							countdown.setText(internalTimer.getFormatTime() + "   " + taskDescription );
							adjustSize(countdown);
						}
					}
					if (Activator.getDefault().isShowDialog()) {
						String message=(timer.getType() == PomodoroTimer.TYPE_WORK ? "Work " : "Pause ") + "Time finished";
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Pomodoro Timer Finished", message);
						setShowDialog(false);
					}

					scheduleTimer(changeInterval);
				} else {
					for (Label countdown : counterLabels) {
						if (!countdown.isDisposed()) {
							countdown.setForeground(new Color(Display.getDefault(), StringConverter.asRGB(preferenceStore.getString(PomodoroPreferencePage.POMODORO_TIME_COLOR))));
							countdown.setText(sdf.format(new Date(timer.getConfigWorkTime())) + "   " + taskDescription );
							adjustSize(countdown);
						}
					}
					scheduleTimer(changeInterval);
				}

			}
		});
	}

	protected void adjustSize(Label countdown) {
		GC gc = new GC(countdown);
		FontMetrics fm = gc.getFontMetrics();
		int charWidth = fm.getAverageCharWidth();
		int width = countdown.computeSize(charWidth	* countdown.getText().length(),	SWT.DEFAULT).x;
		((GridData) countdown.getLayoutData()).widthHint = width;
		countdown.getParent().getParent().layout(true, true);
	}

	public void checkTimerStatus() {
		TimerTask task=new TimerTask() {
			@Override
			public void run() {
				PomodoroTimer newTimer;
				if (timer == null || timer.isInterrupted()) {
					return;
				}
				if (timer.getStatus().equals(PomodoroTimer.STATUS_FINISHED)) {
					setShowDialog(true);
					while (isShowDialog()) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Log.INSTANCE.logError("Error sleeping Thread", e);
						}
					}
					UIUtil.showReceivedMessages();
					IPreferenceStore preferenceStore=getPreferenceStore();
					if (timer != null && timer.getType() == PomodoroTimer.TYPE_WORK) {
						int pauseTimer=preferenceStore.getInt(PomodoroPreferencePage.POMODORO_PAUSE) * 60 * 1000;
						newTimer=createTimer(pauseTimer, PomodoroTimer.TYPE_PAUSE);
						if (preferenceStore.getBoolean(PomodoroPreferencePage.WORK_PAUSE_AUTO_SWITCH)) {
							newTimer.start();
						}
					} else {
						newTimer=createTimer(timer.getConfigWorkTime(), PomodoroTimer.TYPE_WORK);
					}
				}

			}

		};
		scheduler.schedule(task, 1000, 1000);
	}

	public void checkMessages() {
		Job job=new Job("AddReceiver") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				while (Job.getJobManager().currentJob() != null && Job.getJobManager().currentJob().getName().equals("ConnectToJGroups")) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Log.INSTANCE.logError("Error sleeping Thread", e);
					}
				}
				communication.setReceiver(new ReceiverAdapter() {
					@Override
					public void receive(final Message msg) {
						if (msg != null && (msg.getObject() instanceof AbstractPomodoroMessage)) {

							if (msg.getObject() instanceof TimerMessage) {
								TimerMessage tm=(TimerMessage) msg.getObject();
								if (receivedMessages.contains(tm)) {
									receivedMessages.remove(tm);
								}
								receivedMessages.add(tm);
							} else if (msg.getObject() instanceof TextMessage) {
								communication.addReceivedMessage((TextMessage) msg.getObject());
								if (!timer.getStatus().equals(PomodoroTimer.STATUS_WORKING_TIME)) {
									UIUtil.showReceivedMessages();
								}

							}
							Display display=Display.getDefault();
							if (display != null && !display.isDisposed()) {
								for (Runnable listener : listeners) {
									display.asyncExec(listener);
								}
							}
						} else {
							Log.INSTANCE.logWarning("Error sleeping Thread");
						}
					}
				});
				return Status.OK_STATUS;
			}

		};
		job.setUser(false);
		job.setRule(getRule());
		job.schedule();
	}

	public List<TimerMessage> getReceivedMessages() {
		return receivedMessages;
	}

	private final List<Runnable> listeners=Collections.synchronizedList(new ArrayList<Runnable>());

	public void addCommunicationListener(Runnable listener) {
		listeners.add(listener);
	}

	public void removeCommunicationListener(Runnable listener) {
		listeners.remove(listener);
	}

	public String getTaskDescription() {
		return taskDescription;
	}

	public void setTaskDescription(String taskDescription) {
		this.taskDescription = taskDescription;
	}
}
