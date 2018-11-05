package com.ice2systems.voice.voice_assist;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

/**
 * Hello world!
 *
 */
public class VoiceAssistent 
{		
		final ResourceProvider provider;
		final static long MONOLOG_PAUSE = 200;
	
		public VoiceAssistent(final String workingDir, final String title) {
			provider = new ResourceProvider(workingDir, title);
		}

		private long getShift(final JSONObject time) {
			long h = (Long)time.get("h");
			long m = (Long)time.get("m");
			long s = (Long)time.get("s");
			long ms = (Long)time.get("ms");		
			
			return h*3600000 + m*60000 + s*1000 + ms;
		}
		
		private List<PlayList> createPlayList(JSONObject jsonContent) throws IOException {
			List<PlayList> list = new LinkedList<PlayList>();
			
			JSONArray array = (JSONArray)jsonContent.get("content");
			long correction = 0;

			for(int i=0;i<array.size();i++) {
				System.out.println(String.format("loading %d/%d", i+1, array.size()));
				
				JSONObject item = (JSONObject)array.get(i);
				JSONArray voices = (JSONArray)item.get("voices");
				
				long shift = getShift((JSONObject)item.get("startTS"));
				PlayList pl = new PlayList((Long)item.get("id"), shift);
				
				for(int j=0;j<voices.size();j++) {
					String name = (String)voices.get(j);
					pl.add2List(provider.getMedia(name));
				}
				
				if( i == 0 ) {
					correction = pl.shift;
				}

				pl.relativeShift = pl.shift - correction;
				
				list.add(pl);
			}
			
			return list;
		}
		
		private void say(InputStream speechStream) throws JavaLayerException {
			AdvancedPlayer player = new AdvancedPlayer(speechStream,
					javazoom.jl.player.FactoryRegistry.systemRegistry().createAudioDevice());
	
			player.setPlayBackListener(new PlaybackListener() {
				@Override
				public void playbackStarted(PlaybackEvent evt) {
				}
				
				@Override
				public void playbackFinished(PlaybackEvent evt) {
				}
			});
			
			player.play();			
		}
		
		private void play(final List<PlayList> list) throws JavaLayerException, InterruptedException, IOException {
			long timeSpent = 0;
			
			for(PlayList item: list) {
				if(timeSpent == 0) {//very first monolog
					System.out.println("\"\\nPress press ENTER when see the very first subtitles on the screen.\\n\"");
					System.in.read();
				}
				
				long startTS = System.currentTimeMillis();
				long delay = item.relativeShift - timeSpent;
				
				System.out.println(String.format("id=%d delay=%d",item.id,delay));
				
				if(delay>0) {
					Thread.sleep(delay);
				}
				
				int i = 0;
				for(InputStream speechStream: item.list) {
					if(i>0) {// delay between monologs in the same set
						Thread.sleep(MONOLOG_PAUSE);
					}
					say(speechStream);
					i++;
				}
				
				timeSpent += (System.currentTimeMillis() - startTS);
			}
		}
		
    public static void main( String[] args )
    {
    	 if( args.length < 2) {
    		 System.out.println("Usage: <working directory for caching> <title name>");
    		 System.exit(-1);
    	 }
    	 
       VoiceAssistent voiceAssist = new VoiceAssistent(args[0], args[1]);
       
       try {
      	 
     		
      	 JSONParser parser = new JSONParser();
     
      	 Object content = parser.parse(voiceAssist.provider.getDescriptor());
      	 
      	 JSONObject jsonContent = (JSONObject)content;

         System.out.println("Creating playlist");
         
         List<PlayList> list = voiceAssist.createPlayList(jsonContent);
         
         System.out.println("Start playing");
         
         voiceAssist.play(list);
         
       } catch (IOException e) {
				 e.printStackTrace();
       } catch (ParseException e) {
				e.printStackTrace();
			} catch (JavaLayerException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    }
}
