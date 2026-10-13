package bms.player.beatoraja.launcher;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bms.player.beatoraja.IRConfig;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.ir.IRConnectionManager;
import bms.player.beatoraja.launcher.PlayConfigurationView.OptionListCell;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class IRConfigurationView implements Initializable {
	private static final Logger logger = LoggerFactory.getLogger(IRConfigurationView.class);
	
	@FXML
	private Button primarybutton;
	@FXML
	private ComboBox<String> irname;
	@FXML
	private Hyperlink irhome;
	@FXML
	private TextField iruserid;
	@FXML
	private PasswordField irpassword;
	@FXML
	private ComboBox<Integer> irsend;
	@FXML
	private CheckBox importrival;
	@FXML
	private CheckBox importscore;
	@FXML
	private CheckBox bmsirArenaEnabled;
	@FXML
	private TextField bmsirArenaServer;
	
	private Map<String, IRConfig> irmap = new HashMap<String, IRConfig>();
	
	private String primary;
	
	private IRConfig currentir;
	
	private PlayerConfig player;

	private void initComboBox(ComboBox<Integer> combo, final String[] values) {
		combo.setCellFactory((param) -> new OptionListCell(values));
		combo.setButtonCell(new OptionListCell(values));
		for (int i = 0; i < values.length; i++) {
			combo.getItems().add(i);
		}
	}
	public void initialize(URL arg0, ResourceBundle arg1) {
		initComboBox(irsend, new String[] { arg1.getString("IR_SEND_ALWAYS"), arg1.getString("IR_SEND_FINISH"), arg1.getString("IR_SEND_UPDATE")});
		irname.getItems().setAll(IRConnectionManager.getAllAvailableIRConnectionName());
	}
	
    public void update(PlayerConfig player) {
    	this.player = player;
        currentir = null;
        irmap.clear();
        irname.getItems().setAll(availableAndStoredNames(IRConnectionManager.getAllAvailableIRConnectionName(), player.getIrconfig()));
    	
    	for(IRConfig ir : player.getIrconfig()) {
    		irmap.put(ir.getIrname(), ir);
    	}
    	    	
		primary = player.getIrconfig().length > 0 ? player.getIrconfig()[0].getIrname() : null;
		if(!irname.getItems().contains(primary)) {
			if (irname.getItems().size() == 0) {
				primary = null;
			} else {
				primary = irname.getItems().get(0);
			}
		}
		irname.setValue(primary);
		updateIRConnection();
		bmsirArenaEnabled.setSelected(player.isBmsirArenaEnabled());
		bmsirArenaServer.setText(player.getBmsirArenaServer());

    }

    public void commit() {
		updateIRConnection();
		
        player.setIrconfig(configurationsToSave(irname.getItems(), irmap, primary));
		player.setBmsirArenaEnabled(bmsirArenaEnabled.isSelected());
		player.setBmsirArenaServer(bmsirArenaServer.getText());
    }
    
    static List<String> availableAndStoredNames(String[] available, IRConfig[] saved) {
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>(java.util.Arrays.asList(available));
        for (IRConfig config : saved) if (config != null && config.getIrname() != null && !config.getIrname().isBlank()) names.add(config.getIrname());
        return new ArrayList<>(names);
    }
    static IRConfig[] configurationsToSave(List<String> names, Map<String, IRConfig> configs, String primary) {
        List<IRConfig> result = new ArrayList<>();
        for (String name : names) {
            IRConfig config = configs.get(name);
            if (config == null || !config.validate()) continue;
            if (name.equals(primary)) result.add(0, config); else result.add(config);
        }
        return result.toArray(IRConfig[]::new);
    }

	@FXML
	public void setPrimary() {
		primary = irname.getValue();
		updateIRConnection();
	}

	@FXML
	public void updateIRConnection() {
		if(currentir != null) {
			currentir.setUserid(iruserid.getText());
			currentir.setPassword(irpassword.getText());
			currentir.setIrsend(irsend.getValue());
			currentir.setImportscore(importscore.isSelected());
			currentir.setImportrival(importrival.isSelected());
		}
		
        if (irname.getValue() == null) {
            currentir = null;
            return;
        }
    	String homeurl = IRConnectionManager.getHomeURL(irname.getValue());
        irhome.setDisable(homeurl == null || homeurl.isBlank());
		irhome.setText(homeurl);
		irhome.setOnAction((event) -> {
            Desktop desktop = Desktop.getDesktop();
            URI uri;
            try {
                uri = new URI(homeurl);
                desktop.browse(uri);
            } catch (Exception e) {
				logger.warn("最新版URLアクセス時例外:{}", e.getMessage());
            }
        });
		
		if(!irmap.containsKey(irname.getValue())) {
			IRConfig ir = new IRConfig();
			ir.setIrname(irname.getValue());
			irmap.put(irname.getValue(), ir);
		}
		currentir = irmap.get(irname.getValue());
		iruserid.setText(currentir.getUserid());
		irpassword.setText(currentir.getPassword());
		irsend.setValue(currentir.getIrsend());
		importscore.setSelected(currentir.isImportscore());
		importrival.setSelected(currentir.isImportrival());

		primarybutton.setVisible(!(primary != null && irname.getValue().equals(primary)));
	}
}
