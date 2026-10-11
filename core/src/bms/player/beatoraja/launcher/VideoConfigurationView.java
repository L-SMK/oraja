package bms.player.beatoraja.launcher;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainLoader;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.Resolution;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.application.Platform;
import javafx.util.StringConverter;
import bms.player.beatoraja.camera.FFmpegCameraProcess;
import javafx.util.Pair;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class VideoConfigurationView implements Initializable {
	@FXML
	private ComboBox<Resolution> resolution;
	@FXML
	private ComboBox<Config.DisplayMode> displayMode;
	@FXML
	private ComboBox<String> bgaOp;
	@FXML
	private ComboBox<String> bgaExpand;
    @FXML private ComboBox<bms.player.beatoraja.play.bga.BgaQualityProfile.Mode> bgaQuality;
    @FXML private CheckBox bgaAsyncPipeline;
    @FXML private CheckBox bgaPerformanceStats;
    @FXML private CheckBox handCameraEnabled;
    @FXML private ComboBox<FFmpegCameraProcess.Device> handCameraDevice;
    @FXML private Button handCameraRefresh;
    @FXML private Label handCameraStatus;
    @FXML private Spinner<Integer> handCameraWidth, handCameraHeight, handCameraFps;
    @FXML private CheckBox handCameraMirror;
    private ResourceBundle resources;

	@FXML
	private CheckBox vSync;

	@FXML
	private Spinner<Integer> maxFps;
	@FXML
	private Spinner<Integer> missLayerTime;

	@FXML
	private ComboBox<String> monitor;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.resources = resources;
        handCameraDevice.setConverter(new StringConverter<>() {
            public String toString(FFmpegCameraProcess.Device device) { return device == null ? "" : device.label(); }
            public FFmpegCameraProcess.Device fromString(String text) {
                String value = text == null ? "" : text.trim();
                return handCameraDevice.getItems().stream()
                        .filter(device -> device.label().equals(value) || device.value().equals(value)).findFirst()
                        .orElse(new FFmpegCameraProcess.Device(value, value));
            }
        });
		updateResolutions();
        bgaQuality.getItems().setAll(bms.player.beatoraja.play.bga.BgaQualityProfile.Mode.values());

		displayMode.getItems().setAll(Config.DisplayMode.values());
		monitor.getItems().setAll(Arrays.stream(Lwjgl3ApplicationConfiguration.getMonitors()).map(monitor -> String.format("%s [%s, %s]", monitor.name, Integer.toString(monitor.virtualX), Integer.toString(monitor.virtualY))).toList());
    }

    public void update(Config config) {
        handCameraEnabled.setSelected(config.isHandCameraEnabled());
        String device = config.getHandCameraDevice();
        handCameraDevice.setValue(new FFmpegCameraProcess.Device(device, device));
        handCameraWidth.getValueFactory().setValue(config.getHandCameraWidth());
        handCameraHeight.getValueFactory().setValue(config.getHandCameraHeight());
        handCameraFps.getValueFactory().setValue(config.getHandCameraFps());
        handCameraMirror.setSelected(config.isHandCameraMirror());
		displayMode.setValue(config.getDisplaymode());
		resolution.setValue(config.getResolution());
		vSync.setSelected(config.isVsync());
		monitor.setValue(config.getMonitorName());
		bgaOp.getSelectionModel().select(config.getBga());
		bgaExpand.getSelectionModel().select(config.getBgaExpand());
        bgaQuality.setValue(config.getBgaQualityMode());
        bgaAsyncPipeline.setSelected(config.isBgaAsyncPipeline());
        bgaPerformanceStats.setSelected(config.isBgaShowPerformanceStats());
		maxFps.getValueFactory().setValue(config.getMaxFramePerSecond());
	}

	public void updatePlayer(PlayerConfig player) {
		missLayerTime.getValueFactory().setValue(player.getMisslayerDuration());
	}

	public void commit(Config config) {
        config.setHandCameraEnabled(handCameraEnabled.isSelected());
        config.setHandCameraDevice(handCameraDevice.getConverter().fromString(handCameraDevice.getEditor().getText()).value());
        config.setHandCameraWidth(handCameraWidth.getValue());
        config.setHandCameraHeight(handCameraHeight.getValue());
        config.setHandCameraFps(handCameraFps.getValue());
        config.setHandCameraMirror(handCameraMirror.isSelected());
		config.setResolution(resolution.getValue());
		config.setDisplaymode(displayMode.getValue());
		config.setVsync(vSync.isSelected());
		config.setMonitorName(monitor.getValue());
		config.setBga(bgaOp.getSelectionModel().getSelectedIndex());
		config.setBgaExpand(bgaExpand.getSelectionModel().getSelectedIndex());
        config.setBgaQualityMode(bgaQuality.getValue());
        config.setBgaAsyncPipeline(bgaAsyncPipeline.isSelected());
        config.setBgaShowPerformanceStats(bgaPerformanceStats.isSelected());
		config.setMaxFramePerSecond(maxFps.getValue());
	}

	public void commitPlayer(PlayerConfig player) {
		player.setMisslayerDuration(missLayerTime.getValue());
	}

	@FXML
	public void refreshHandCameras() {
        var selected = handCameraDevice.getConverter().fromString(handCameraDevice.getEditor().getText());
        handCameraRefresh.setDisable(true);
        handCameraDevice.setDisable(true);
        handCameraStatus.setText(resources.getString("HAND_CAMERA_LOADING"));
        Thread thread = new Thread(() -> {
            try {
                var devices = FFmpegCameraProcess.devices();
                Platform.runLater(() -> {
                    handCameraDevice.getItems().setAll(devices);
                    handCameraDevice.setValue(devices.stream().filter(device -> device.value().equals(selected.value()))
                            .findFirst().orElse(selected));
                    handCameraStatus.setText(resources.getString(devices.isEmpty()
                            ? "HAND_CAMERA_EMPTY" : "HAND_CAMERA_READY"));
                    handCameraRefresh.setDisable(false);
                    handCameraDevice.setDisable(false);
                });
            } catch (Exception | LinkageError error) {
                Platform.runLater(() -> {
                    handCameraStatus.setText(resources.getString("HAND_CAMERA_FAILED"));
                    handCameraRefresh.setDisable(false);
                    handCameraDevice.setDisable(false);
                });
            }
        }, "hand-camera-device-list");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
	public void updateResolutions() {
		Resolution oldValue = resolution.getValue();
		resolution.getItems().clear();

		if (displayMode.getValue() == Config.DisplayMode.FULLSCREEN) {
			Graphics.DisplayMode[] displays = MainLoader.getAvailableDisplayMode();
			for(Resolution r : Resolution.values()) {
				for(Graphics.DisplayMode display : displays) {
					if(display.width == r.width && display.height == r.height) {
						resolution.getItems().add(r);
						break;
					}
				}
			}
		} else {
			Graphics.DisplayMode display = MainLoader.getDesktopDisplayMode();
			for(Resolution r : Resolution.values()) {
				if (r.width <= display.width && r.height <= display.height) {
					resolution.getItems().add(r);
				}
			}
		}
		resolution.setValue(resolution.getItems().contains(oldValue)
				? oldValue : resolution.getItems().get(resolution.getItems().size() - 1));
	}
}
