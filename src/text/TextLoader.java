package text;

import javafx.scene.control.Alert;

import java.io.*;

public class TextLoader
{
	public static String loadExternal(File file)
	{
		try
		{
			return inputStreamToString(new FileInputStream(file));
		}
		catch (FileNotFoundException e)
		{
			return null;
		}
	}

	public static String loadInternal(String filename)
	{
		return inputStreamToString(TextLoader.class.getClassLoader().getResourceAsStream("text/" + filename));
	}

	private static String inputStreamToString(InputStream inputStream)
	{
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

		StringBuilder stringBuilder = new StringBuilder();
		String line;
		try
		{
			while (true)
			{
				line = bufferedReader.readLine();
				if (line == null)
				{
					break;
				}

				stringBuilder.append(line);
				stringBuilder.append('\n');
			}
		}
		catch (IOException e)
		{
			return null;
		}

		return stringBuilder.toString();
	}
}
