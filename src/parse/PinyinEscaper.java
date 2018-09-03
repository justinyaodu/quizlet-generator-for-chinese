package parse;

class PinyinEscaper
{
	private static String[][] replacements =
			{
					{"a1", "ā"},
					{"a2", "á"},
					{"a3", "ǎ"},
					{"a4", "à"},
					{"e1", "ē"},
					{"e2", "é"},
					{"e3", "ě"},
					{"e4", "è"},
					{"i1", "ī"},
					{"i2", "í"},
					{"i3", "ǐ"},
					{"i4", "ì"},
					{"o1", "ō"},
					{"o2", "ó"},
					{"o3", "ǒ"},
					{"o4", "ò"},
					{"u1", "ū"},
					{"u2", "ú"},
					{"u3", "ǔ"},
					{"u4", "ù"},
					{"v1", "ǖ"},
					{"v2", "ǘ"},
					{"v3", "ǚ"},
					{"v4", "ǜ"},
					{"v", "ü"}
			};

	static String escape(String pinyin)
	{
		for (String[] replacement : replacements)
		{
			pinyin = pinyin.replaceAll(replacement[0], replacement[1]);
		}

		return pinyin;
	}
}