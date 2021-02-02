package esb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.impl.biome.modification.BiomeSelectionContextImpl;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Predicate;

public class Mod implements ModInitializer
{
	public static Gson GSON = new GsonBuilder()
			.registerTypeAdapter(Identifier.class, new IdentifierTypeAdapter())
			.setPrettyPrinting().create();

	public static Config CONFIG = new Config();
	public static final String CONFIG_LOCATION = "config/spawnable_biomes.json";
	public static final String MODID = "make-every-biome-spawnable-lol";

	@Override
	public void onInitialize()
	{
		readConfig();

		Predicate<BiomeSelectionContext> NORMAL_SPAWNS = (b)->{
			return b.getBiome().getSpawnSettings().isPlayerSpawnFriendly();
		};

		Predicate<BiomeSelectionContext> VALID_BIOMES = (b)->
		{
			Identifier id = b.getBiomeKey().getValue();
			for(Identifier i : CONFIG.BIOMES)
			{
				if(i.equals(id))
				{
					return CONFIG.IS_WHITELIST;
				}
			}
			return !CONFIG.IS_WHITELIST;
		};


		BiomeModifications.create(new Identifier(MODID, "disable_spawns"))
				.add(ModificationPhase.POST_PROCESSING, NORMAL_SPAWNS, (s)->{
					s.getSpawnSettings().setPlayerSpawnFriendly(false);
				});

		BiomeModifications.create(new Identifier(MODID, "spawns"))
				.add(ModificationPhase.POST_PROCESSING, VALID_BIOMES, (s)->{
					s.getSpawnSettings().setPlayerSpawnFriendly(true);
				});
	}

	private void readConfig()
	{
		File cfgFile = new File(CONFIG_LOCATION).getAbsoluteFile();
		if(cfgFile.exists())
		{
			try
			{
				JsonReader reader  = new JsonReader(new FileReader(CONFIG_LOCATION));
				CONFIG = GSON.fromJson(reader, Config.class);
			}
			catch(FileNotFoundException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			try
			{
				String cfgOut = GSON.toJson(CONFIG);
				Files.write(Paths.get(CONFIG_LOCATION), cfgOut.getBytes());
			}
			catch(IOException ioException)
			{
				ioException.printStackTrace();
			}
		}
	}

	public static class Config
	{
		public boolean IS_WHITELIST = false;
		public Identifier[] BIOMES = {};
	}

	public static class IdentifierTypeAdapter extends TypeAdapter<Identifier>
	{

		@Override
		public void write(JsonWriter out, Identifier value) throws IOException
		{
			out.value(value.toString());
		}

		@Override
		public Identifier read(JsonReader in) throws IOException
		{
			return new Identifier(in.nextString());
		}
	}
}
