package bms.player.beatoraja.input;

import bms.player.beatoraja.PlayModeConfig.MidiConfig;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * MIDIデバイス入力処理用クラス
 *
 * @author excln
 */
public final class MidiInputProcessor extends BMSPlayerInputDevice implements AutoCloseable {

	static final int MaxKeys = 128;

	ArrayList<MidiDevice> devices = new ArrayList<>();
	MidiReceiver receiver = new MidiReceiver();

	// milliseconds
	long starttime = 0;

	int pitch = 0;

	boolean lastPressedKeyAvailable = false;
	MidiConfig.Input lastPressedKey = new MidiConfig.Input();
	private volatile boolean diffFilterPressed;

	// pitch value: -8192 ~ 8191
	final int pitchThreshold = 8192 / 32;

	// MIDI note number -> game key number
	// NOTE: この方法だと1つのMIDIキーに複数キー割り当てが不可能
	KeyHandler[] keyMap = new KeyHandler[MaxKeys];
    private final ArrayList<Object> heldSources = new ArrayList<>();

	KeyHandler pitchBendUp, pitchBendDown;

	public MidiInputProcessor(BMSPlayerInputProcessor inputProcessor) {
		super(inputProcessor, Type.MIDI);
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for (MidiDevice.Info info : infos) {
			try {
				MidiDevice device = MidiSystem.getMidiDevice(info);
				devices.add(device);
			} catch (MidiUnavailableException e) {
			}
		}

		clearHandlers();
	}

	public void open() {
		devices.forEach(device -> {
			try {
				device.open();
				device.getTransmitter().setReceiver(receiver);
			} catch (MidiUnavailableException e) {
			}			
		});
	}

	public void close() {
		devices.forEach(MidiDevice::close);
	}

    public void setConfig(MidiConfig config) { setConfig(config, false); }
    public void setConfig(MidiConfig config, boolean multi) {
        clear();
        clearHandlers();
        heldSources.clear();
        MidiConfig.Input[] keys = config.getKeys();
        for (int index = 0; index < keys.length + 2; index++) {
            final int logical = index;
            MidiConfig.Input primary = index < keys.length ? keys[index]
                    : (index == keys.length ? config.getStart() : config.getSelect());
            ArrayList<MidiConfig.Input> bindings = new ArrayList<>();
            if (primary != null) bindings.add(primary);
            MidiConfig.Input[][] additional = config.getAdditionalBindings();
            if (multi && additional != null && index < additional.length && additional[index] != null) {
                bindings.addAll(Arrays.asList(additional[index]));
            }
            for (MidiConfig.Input binding : bindings) {
                Object source = new Object();
                heldSources.add(source);
                setHandler(binding, pressed -> {
                    if (logical < keys.length) {
                        bmsPlayerInputProcessor.keyChanged(source, this, currentTime(), logical, pressed);
                        bmsPlayerInputProcessor.setAnalogState(logical, false, 0);
                    } else if (logical == keys.length) bmsPlayerInputProcessor.startChanged(source, pressed);
                    else bmsPlayerInputProcessor.selectChanged(source, pressed);
                });
            }
        }
        setHandler(config.getDiffFilter(), pressed -> diffFilterPressed = pressed);
    }

	public void setStartTime(long starttime) {
		this.starttime = starttime;
	}

	public void clear() {
        for (Object source : heldSources) bmsPlayerInputProcessor.releaseSource(source);
		lastPressedKeyAvailable = false;
		diffFilterPressed = false;
	}

	public boolean isDiffFilterPressed() {
		return diffFilterPressed;
	}

	public void clearHandlers() {
		Arrays.fill(keyMap, null);
		pitchBendUp = null;
		pitchBendDown = null;
	}

	void setHandler(MidiConfig.Input input, KeyHandler handler) {
		if (input == null || input.type == null)
			return;
		switch (input.type) {
			case NOTE:
				if (input.value >= 0 && input.value < MaxKeys && keyMap[input.value] == null) {
					keyMap[input.value] = handler;
				}
				break;
			case PITCH_BEND:
				if (input.value > 0 && pitchBendUp == null) {
					pitchBendUp = handler;
				} else if (input.value < 0 && pitchBendDown == null) {
					pitchBendDown = handler;
				}
				break;
			case CONTROL_CHANGE:
				break;
		}
	}

	void noteOff(int num) {
		if (keyMap[num] != null) {
			keyMap[num].accept(false);
		}
	}

	void noteOn(int num) {
		lastPressedKeyAvailable = true;
		lastPressedKey.type = MidiConfig.Input.Type.NOTE;
		lastPressedKey.value = num;
		if (keyMap[num] != null) {
			keyMap[num].accept(true);
		}
	}

	void onPitchBendUp(boolean pressed) {
		if (pressed) {
			lastPressedKeyAvailable = true;
			lastPressedKey.type = MidiConfig.Input.Type.PITCH_BEND;
			lastPressedKey.value = 1;
		}
		if (pitchBendUp != null) {
			pitchBendUp.accept(pressed);
		}
	}

	void onPitchBendDown(boolean pressed) {
		if (pressed) {
			lastPressedKeyAvailable = true;
			lastPressedKey.type = MidiConfig.Input.Type.PITCH_BEND;
			lastPressedKey.value = -1;
		}
		if (pitchBendDown != null) {
			pitchBendDown.accept(pressed);
		}
	}

	long currentTime() {
		return System.nanoTime() / 1000 - starttime;
	}

	public boolean hasLastPressedKey() {
		return lastPressedKeyAvailable;
	}

	public MidiConfig.Input getLastPressedKey() {
		return lastPressedKeyAvailable ? new MidiConfig.Input(lastPressedKey) : null;
	}

	public void clearLastPressedKey() {
		lastPressedKeyAvailable = false;
	}

	private final class MidiReceiver implements Receiver {

		public void send(MidiMessage message, long timeStamp) {
			if (message instanceof ShortMessage) {
				ShortMessage sm = (ShortMessage)message;
				switch (sm.getCommand()) {
					case ShortMessage.NOTE_OFF:
						noteOff(sm.getData1());
						break;
					case ShortMessage.NOTE_ON:
						if (sm.getData2() == 0) {
							noteOff(sm.getData1());
						} else {
							noteOn(sm.getData1());
						}
						break;
					case ShortMessage.PITCH_BEND: {
						int newPitch = (int)(short)((sm.getData1() & 0x7f) | ((sm.getData2() & 0x7f) << 7)) - 0x2000;
						if (newPitch > pitchThreshold) {
							if (pitch < -pitchThreshold) {
								onPitchBendDown(false);
							}
							if (pitch <= pitchThreshold) {
								onPitchBendUp(true);
							}
						} else if (newPitch < -pitchThreshold) {
							if (pitch > pitchThreshold) {
								onPitchBendUp(false);
							}
							if (pitch >= -pitchThreshold) {
								onPitchBendDown(true);
							}
						} else {
							if (pitch > pitchThreshold) {
								onPitchBendUp(false);
							}
							if (pitch < -pitchThreshold) {
								onPitchBendDown(false);
							}
						}
						pitch = newPitch;
						break;
					}
				}
			}
		}

		public void close(){
		}
	}
	
	private interface KeyHandler {
		
		public void accept(boolean pressed);
	}
}
