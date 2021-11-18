from enum import Enum
import os

FEATURES_DIR = "src/main/java/net/como/client/cheats"
TARGET_PATH = "FEATURES.md"
NOT_PRESENT = "No description available as of yet."

class NotJavaPath(Exception):
    """The path specified was not a Java file and hence must not be a feature."""

class SettingType(Enum):
    STRING = 1,
    STR_BOOL_HASHMAP = 2,
    FLOAT = 3,
    BOOLEAN = 4,
    OTHER = 5,
    INTEGER = 6

class Setting:
    def __init__(self, name, default) -> None:
        self.__name = name
        self.__default = default

    def get_type(self):
        v = self.get_default_value()

        if v.startswith("\"") and v.endswith("\""):
            return SettingType.STRING
        
        # Floats/Doubles
        if (v.endswith("d") or v.endswith("f")) and v[:-1].replace('.','',1).isdigit():
            return SettingType.FLOAT

        # Ints
        if v.isdigit():
            return SettingType.INTEGER

        # Boolean
        if v == "true" or v == "false":
            return SettingType.BOOLEAN

        # HashMap thing
        if v == "new HashMap<String, Boolean>()":
            return SettingType.STR_BOOL_HASHMAP
        
        return SettingType.OTHER

    def get_name(self):
        return self.__name

    def get_default_value(self):
        return self.__default

        # TODO add this
    def get_parsed_default(self):
        return self.get_default_value()

    @staticmethod
    def from_line(line = str()):
        line = line.strip()

        line = line.replace("this.addSetting(new Setting(", "")
        line = line.replace("));", "")

        # Parse the name
        name = line[1:]
        name = name[:name.find("\"")]

        # Parse the value
        default = line[len("\"\",") + len(name):].strip()

        return Setting(name, default)

class Feature:
    # Just incase we want to easily change it in the future.
    __desc_selector = "this.description = \""

    def __init__(self, path):
        if not path.endswith(".java"):
            raise NotJavaPath(path)

        self.__path = path

    def get_name(self):
        return os.path.basename(self.__path)[:-len(".java")]

    def __get_code(self):
        f = open(self.__path)
        code = f.read()
        f.close()

        return code

    def __desc_index(self, code):
        if code == None:
            code = self.__get_code()

        i = -1
        try:
            i = code.index(self.__desc_selector)
        except:
            pass

        return i

    def get_description(self, default):
        code = self.__get_code()
        start = self.__desc_index(code)

        if start == -1: return default

        start += len(self.__desc_selector)
        
        # Get all of the string util we finish the line
        c = ""
        i = 0

        # This might cause issues since I might do something weird rather than ";\n at the end of a string.
        # So just be warned
        while not c.endswith("\";\n"):
            c += code[start + i]
            i += 1

        # Remove ";
        c = c[:-3]

        # Add a fullstop
        c + "." if not c.endswith(".") else c

        return c
    
    # TODO maybe get the settings
    # TODO add command to activate the cheat
    # TODO populate the file with screenshots

    def get_readme_line(self, not_present) -> str():
        line = ""

        # Display basic information
        line += f"## {self.get_name()}\n"
        line += f"[(Source Code)]({self.__path}) "
        line += self.get_description(not_present) + "\n"

        # Display settings
        settings = self.get_settings()
        if len(settings) > 0:
            line += "### Settings\n"
            for setting in settings:
                line += f" - {setting.get_name()}: *{setting.get_parsed_default()}*\n"

        return line

    def get_settings(self):
        code = self.__get_code()
        selector = "this.addSetting(new Setting("

        settings = []
        
        i = code.find(selector)
        while i != -1:
            part = c =''
            j = 0

            while c != ';':
                c = code[i + j]
                part += c

                j += 1
            settings.append(Setting.from_line(part))

            i = code.find(selector, i + 1)

        return settings

features = [Feature(os.path.join(FEATURES_DIR, i)) for i in os.listdir(FEATURES_DIR)]

print(f"Detected {len(features)} features... Generating Feature List...")
output = '''# List of Features\n'''
for feature in features:
    output += feature.get_readme_line(NOT_PRESENT) + '\n'

print(output)

print(f"Saving to {TARGET_PATH}...")
f = open(TARGET_PATH, 'w')
f.write(output)
f.close()
print("Finished.")