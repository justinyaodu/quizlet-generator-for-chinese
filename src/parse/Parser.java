package parse;

import java.util.ArrayList;
import java.util.HashMap;

public class Parser
{
	private HashMap<String, Group> groups = new HashMap<>();
	private Group group = null;

	private String output;

	public Parser(String[] lines)
	{
		StringBuilder stringBuilder = new StringBuilder();

		for (int i = 0; i <= lines.length; i++)
		{
			String line = i == lines.length ? "#endfile" : lines[i];
			CommandResult result = parse(line);

			if (result.ERROR)
			{
				output = String.format("Error on line %d: %s\n\n%s", i + 1, result.TEXT, line);
				return;
			}
			else
			{
				stringBuilder.append(result.TEXT);
			}

			output = stringBuilder.toString();
		}
	}

	public String getOutput()
	{
		return output;
	}

	private CommandResult parse(String line)
	{
		//ignore empty lines
		if (line.length() == 0)
		{
			return CommandResult.OK;
		}

		//line is a parser command
		if (line.charAt(0) == '#')
		{
			String[] words = line.split(" ");

			switch (words[0])
			{
				case "##": //line comment, ignore
				{
					return CommandResult.OK;
				}
				case "#group":
				{
					if (group != null) //already inside a group
					{
						return CommandResult.error("cannot open new group, already inside group");
					}
					else if (words.length < 2)
					{
						return CommandResult.error("no group name specified");
					}
					else if (words.length > 2)
					{
						return CommandResult.error("too many parameters");
					}
					else if (groups.get(words[1]) != null)
					{
						return CommandResult.error("group already exists: %s", words[1]);
					}
					else
					{
						groups.put(words[1], group = new Group());
						return CommandResult.OK;
					}
				}
				case "#endgroup":
				{
					if (group == null) //already outside a group
					{
						return CommandResult.error("cannot close group, not inside group");
					}
					else if (words.length > 1)
					{
						return CommandResult.error("this command takes no arguments");
					}
					else if (group.dirty())
					{
						return CommandResult.error("group contains an incomplete term (check to make sure each term has corresponding pinyin and definition)");
					}
					else
					{
						group = null;
						return CommandResult.OK;
					}
				}
				case "#showgroup":
				{
					if (group != null)
					{
						return CommandResult.error("this command is not allowed inside a group");
					}
					else if (words.length == 1)
					{
						return CommandResult.error("no group name specified");
					}
					else
					{
						Group group = groups.get(words[1]);

						if (group == null)
						{
							return CommandResult.error("group does not exist: %s", words[1]);
						}
						else
						{
							String[] formats = new String[words.length - 2];
							System.arraycopy(words, 2, formats, 0, words.length - 2);
							return group.display(formats);
						}
					}
				}
				case "#endfile":
				{
					if (group != null)
					{
						return CommandResult.error("group not closed");
					}
					else if (words.length > 1)
					{
						return CommandResult.error("this command takes no arguments");
					}
					else
					{
						return CommandResult.OK;
					}
				}
				default:
				{
					return CommandResult.error("unknown parser command: %s", words[0]);
				}
			}
		}
		else //is a term, pinyin, or definition line
		{
			if (group == null) //no group specified or outside of group
			{
				return CommandResult.error("only parser commands are allowed outside groups");
			}
			else
			{
				group.addLine(line);
				return CommandResult.OK;
			}
		}
	}
}

class Term
{
	private static HashMap<String, String> formatMap = buildFormatMap();

	private static HashMap<String, String> buildFormatMap()
	{
		HashMap<String, String> map = new HashMap<>();
		map.put("reading", "%1$s\t%2$s: %3$s\n");
		map.put("writing", "%3$s\t%2$s: %1$s\n");
		return map;
	}

	private String[] strings;

	Term(String term, String pinyin, String definition)
	{
		strings = new String[]{term, PinyinEscaper.escape(pinyin), definition};
	}

	CommandResult format(String formatName)
	{
		String format = formatMap.get(formatName);

		if (format == null)
		{
			return CommandResult.error("invalid format name specified: %s", formatName);
		}

		return CommandResult.success(format, strings[0], strings[1], strings[2]);
	}
}

class Group
{
	private ArrayList<Term> terms = new ArrayList<>();

	private String term;
	private String pinyin;

	void addLine(String line)
	{
		if (term == null)
		{
			term = line;
		}
		else if (pinyin == null)
		{
			pinyin = line;
		}
		else
		{
			terms.add(new Term(term, pinyin, line));
			term = null;
			pinyin = null;
		}
	}

	//returns true if current term is incomplete
	boolean dirty()
	{
		return term != null;
	}

	//return true if invalid format name encountered
	CommandResult display(String[] formatNames)
	{
		if (formatNames.length == 0)
		{
			return CommandResult.error("no output formats specified");
		}

		StringBuilder builder = new StringBuilder();

		//print terms in requested formats

		for (String formatName : formatNames)
		{
			for (Term term : terms)
			{
				CommandResult result = term.format(formatName);

				if (result.ERROR)
				{
					return result;
				}

				builder.append(result.TEXT);
			}
		}

		return CommandResult.success(builder.toString());
	}
}

class CommandResult
{
	static final CommandResult OK = new CommandResult(false, "");

	final boolean ERROR;
	final String TEXT;

	private CommandResult(boolean error, String format, Object... objects)
	{
		ERROR = error;
		TEXT = String.format(format, objects);
	}

	static CommandResult error(String format, Object... objects)
	{
		return new CommandResult(true, format, objects);
	}

	static CommandResult success(String format, Object... objects)
	{
		return new CommandResult(false, format, objects);
	}
}