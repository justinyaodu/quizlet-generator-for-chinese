package gui;

import javafx.application.Application;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import parse.Parser;
import text.TextLoader;

import java.io.*;
import java.util.Optional;

public class GUI extends Application
{
	private Stage stage;

	private MenuItem newFile;
	private MenuItem saveAs;
	private CheckMenuItem wordWrap = buildWordWrap();

	private TextArea input;
	private TextArea output;

	private SimpleBooleanProperty unsaved = new SimpleBooleanProperty(false);
	private SimpleIntegerProperty fontSizeIndex = new SimpleIntegerProperty(6);
	private SimpleObjectProperty<File> currentFile = new SimpleObjectProperty<>(null);
	private int[] fontSizes = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 44, 48};

	private ButtonType buttonYes = new ButtonType("_Yes", ButtonBar.ButtonData.YES);
	private ButtonType buttonNo = new ButtonType("_No", ButtonBar.ButtonData.NO);
	private ButtonType buttonCancel = new ButtonType("_Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
	private ButtonType buttonOk = new ButtonType("_OK", ButtonBar.ButtonData.OK_DONE);

	@Override
	public void start(Stage stage)
	{
		this.stage = stage;

		stage.titleProperty().bind(buildTitleBinding());
		stage.setOnCloseRequest(buildCloseHandler());

		stage.setScene(buildScene());
		open(TextLoader.loadInternal("demo.txt"), null);

		stage.setWidth(1024);
		stage.setHeight(768);
		stage.show();
	}

	private ObjectBinding<String> buildTitleBinding()
	{
		return new ObjectBinding<String>()
		{
			{
				super.bind(currentFile, unsaved);
			}

			@Override
			protected String computeValue()
			{
				return String.format("Quizlet Generator for Chinese: %s %s",
						currentFile.getValue() == null ? "(new file)" : currentFile.getValue().getName(),
						unsaved.getValue() ? "(unsaved changes)" : "(all changes saved)");
			}
		};
	}

	private EventHandler<WindowEvent> buildCloseHandler()
	{
		return windowEvent ->
		{
			if (doNotOverwrite())
			{
				windowEvent.consume();
			}
		};
	}

	private boolean doNotOverwrite()
	{
		if (!unsaved.get())
		{
			return false;
		}

		Alert alert = new Alert(Alert.AlertType.WARNING, "You have unsaved changes. Would you like to save them?", buttonYes, buttonNo, buttonCancel);
		Optional<ButtonType> button = alert.showAndWait();

		if (button.isPresent())
		{
			if (button.get().equals(buttonYes))
			{
				newFile.fire();
				return false;
			}
			else
			{
				//no means can overwrite
				//cancel means shouldn't overwrite
				return !button.get().equals(buttonNo);
			}
		}
		else
		{
			//dialog closed, do not overwrite
			return true;
		}
	}

	private Scene buildScene()
	{
		return new Scene(buildRoot());
	}

	private Pane buildRoot()
	{
		return new BorderPane(new SplitPane(buildInput(), buildOutput()), buildMenu(), null, null, null);
	}

	private MenuBar buildMenu()
	{
		return new MenuBar(buildFile(), buildView(), buildHelp());
	}

	private Menu buildFile()
	{
		return new Menu("_File", null, buildNew(), buildOpen(), buildSave(), buildSaveAs());
	}

	private MenuItem buildNew()
	{
		newFile = new MenuItem("_New");
		newFile.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
		newFile.setOnAction(actionEvent -> {
			if (doNotOverwrite())
			{
				return;
			}

			open(TextLoader.loadInternal("new.txt"), null);
		});
		return newFile;
	}

	private MenuItem buildOpen()
	{
		MenuItem open = new MenuItem("_Open...");
		open.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
		open.setOnAction(actionEvent ->
		{
			if (doNotOverwrite())
			{
				return;
			}

			FileChooser chooser = new FileChooser();
			chooser.setTitle("Open File");
			File file = chooser.showOpenDialog(stage);
			if (file != null)
			{
				open(TextLoader.loadExternal(file), file);
			}
		});
		return open;
	}

	private void open(String string, File file)
	{
		if (string != null)
		{
			input.setText(string);
			unsaved.set(false);
			currentFile.set(file);
		}
		else
		{
			new Alert(Alert.AlertType.ERROR, "File not found!", buttonOk).showAndWait();
		}
	}

//	private void openInternal(String filename)
//	{
//		openInputStream(GUI.class.getClassLoader().getResourceAsStream("text/" + filename), null);
//	}

//	private void openInputStream(InputStream inputStream, File file)
//	{
//		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//
//		StringBuilder stringBuilder = new StringBuilder();
//		String line;
//		try
//		{
//			while (true)
//			{
//				line = bufferedReader.readLine();
//				if (line == null)
//				{
//					break;
//				}
//
//				stringBuilder.append(line);
//				stringBuilder.append('\n');
//			}
//		}
//		catch (IOException e)
//		{
//			new Alert(Alert.AlertType.ERROR, "File could not be opened!", buttonOk).showAndWait();
//			return;
//		}
//
//		input.setText(stringBuilder.toString());
//		unsaved.set(false);
//		currentFile.set(file);
//	}

	private MenuItem buildSave()
	{
		newFile = new MenuItem("_Save");
		newFile.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
		newFile.setOnAction(actionEvent -> {
			if (currentFile.getValue() != null)
			{
				saveFile(currentFile.get());
			}
			else
			{
				saveAs.fire();
			}
		});
		return newFile;
	}

	private MenuItem buildSaveAs()
	{
		saveAs = new MenuItem("Save _As...");
		saveAs.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+S"));
		saveAs.setOnAction(actionEvent ->
		{
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Save As");
			saveFile(chooser.showSaveDialog(stage));
		});
		return saveAs;
	}

	private void saveFile(File file)
	{
		PrintWriter writer;
		try
		{
			if (!file.getPath().substring(file.getPath().length() - 4).equals(".txt"))
			{
				file = new File(file.getPath() + ".txt");
			}
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));
		}
		catch (NullPointerException e)
		{
			//user canceled save
			return;
		}
		catch (IOException e)
		{
			new Alert(Alert.AlertType.ERROR, "File could not be saved!", buttonOk).showAndWait();
			return;
		}

		writer.print(input.getText());
		writer.flush();
		unsaved.set(false);
		currentFile.set(file);
	}

	private Menu buildView()
	{
		return new Menu("_View", null, buildZoomIn(), buildZoomOut(), wordWrap);
	}

	private MenuItem buildZoomIn()
	{
		MenuItem zoomIn = new MenuItem("Zoom _In");
		zoomIn.setOnAction(actionEvent -> fontSizeIndex.set(Math.max(0, fontSizeIndex.get() - 1)));
		zoomIn.setAccelerator(KeyCombination.keyCombination("Ctrl+-"));
		return zoomIn;
	}

	private MenuItem buildZoomOut()
	{
		MenuItem zoomOut = new MenuItem("Zoom _Out");
		zoomOut.setOnAction(actionEvent -> fontSizeIndex.set(Math.min(fontSizes.length - 1, fontSizeIndex.get() + 1)));
		zoomOut.setAccelerator(KeyCombination.keyCombination("Ctrl+="));
		return zoomOut;
	}

	private CheckMenuItem buildWordWrap()
	{
		wordWrap = new CheckMenuItem("_Word Wrap");
		wordWrap.selectedProperty().set(true);
		return wordWrap;
	}

	private Menu buildHelp()
	{
		return new Menu("_Help", null, buildAbout(), buildLicense());
	}

	private MenuItem buildAbout()
	{
		MenuItem about = new MenuItem("_About...");
		about.setOnAction(actionEvent ->
		{
			Alert alert = new Alert(Alert.AlertType.INFORMATION, null, buttonOk);
			alert.setTitle("About");
			alert.setHeaderText("About Quizlet Generator for Chinese v. 1.0");
			alert.setContentText(TextLoader.loadInternal("about.txt"));
			alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
			alert.show();
		});
		return about;
	}

	private MenuItem buildLicense()
	{
		MenuItem license = new MenuItem("_License...");
		license.setOnAction(actionEvent ->
		{
			Alert alert = new Alert(Alert.AlertType.INFORMATION, null, buttonOk);
			alert.setTitle("License");
			alert.setHeaderText("MIT License");
			alert.setContentText(TextLoader.loadInternal("license.txt"));
			alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
			alert.show();
		});
		return license;
	}

	private TextArea buildInput()
	{
		input = buildTextArea();
		input.textProperty().addListener(observable -> unsaved.set(true));
		return input;
	}

	private TextArea buildOutput()
	{
		output = buildTextArea();
		output.setEditable(false);
		input.textProperty().addListener((observableValue, s, t1) -> output.setText(new Parser(input.getText().split("\n")).getOutput()));
		return output;
	}

	private TextArea buildTextArea()
	{
		TextArea textArea = new TextArea();
		textArea.wrapTextProperty().bind(wordWrap.selectedProperty());
		textArea.setPrefRowCount(Integer.MAX_VALUE);
		textArea.setPrefColumnCount(Integer.MAX_VALUE);
		textArea.fontProperty().bind(new ObjectBinding<Font>()
		{
			{
				super.bind(fontSizeIndex);
			}

			@Override
			protected Font computeValue()
			{
				return new Font(fontSizes[fontSizeIndex.get()]);
			}
		});
		return textArea;
	}

	public static void main(String[] args)
	{
		launch(args);
	}
}
