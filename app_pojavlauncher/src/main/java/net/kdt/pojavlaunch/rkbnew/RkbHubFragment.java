package net.kdt.pojavlaunch.rkbnew;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/** Fresh, self-contained RKB feature hub; does not depend on legacy RKB resource files. */
public final class RkbHubFragment extends Fragment {
    private static final int BG = Color.rgb(5,7,12), PANEL = Color.rgb(9,18,30), BLUE = Color.rgb(0,168,255), MUTED = Color.rgb(150,168,190), WHITE = Color.WHITE;
    private LinearLayout root, body;
    private String page = "Home";

    @Nullable @Override public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        ScrollView scroll = new ScrollView(requireContext()); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        root = new LinearLayout(requireContext()); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(14),dp(16),dp(18)); scroll.addView(root);
        TextView brand = text("RKB LAUNCHER",24,WHITE,true); root.addView(brand);
        TextView sub = text("PLAY  •  EXPLORE  •  CREATE",11,MUTED,true); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2); sp.topMargin=dp(3); root.addView(sub,sp);
        LinearLayout nav = new LinearLayout(requireContext()); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setPadding(0,dp(12),0,dp(10));
        android.widget.HorizontalScrollView navScroll = new android.widget.HorizontalScrollView(requireContext()); navScroll.setHorizontalScrollBarEnabled(false);
        for(String name:new String[]{"Home","Instances","Download","Mods","Cursor","Control","About","Settings"}) { Button b=button(name); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(42)); p.setMargins(dp(2),0,dp(2),0); nav.addView(b,p); b.setOnClickListener(v->{page=name; render();}); }
        navScroll.addView(nav); root.addView(navScroll); body=new LinearLayout(requireContext()); body.setOrientation(LinearLayout.VERTICAL); root.addView(body,new LinearLayout.LayoutParams(-1,-2)); render(); return scroll;
    }
    private void render(){ if(body==null)return; body.removeAllViews(); switch(page){case "Instances": renderInstances(); break; case "Download": renderDownload(); break; case "Mods": renderMods(); break; case "Cursor": renderCursor(); break; case "Control": renderControl(); break; case "About": renderAbout(); break; case "Settings": renderSettings(); break; default: renderHome();} }
    private void renderHome(){ cardTitle("RKB CONTROL CENTER","Launcher tools in one place."); Instance in=null; try{in=Instances.loadSelectedInstance();}catch(Exception ignored){} String instance=in==null?"No instance selected":in.getGameDirectory().getName(); addText("CURRENT INSTANCE",10,BLUE,true); addText(instance,18,WHITE,true); addText("Choose a tool below or return to the original launcher to start Minecraft.",13,MUTED,false); addAction("INSTANCES","Create, install and select Minecraft versions",()->{page="Instances";render();}); addAction("DOWNLOAD CENTER","Browse mods, shaders, resource packs and modpacks",()->{page="Download";render();}); addAction("MOD MANAGER","Enable or disable installed mods",()->{page="Mods";render();}); addAction("CURSOR STUDIO","Adjust RKB cursor preferences",()->{page="Cursor";render();}); addAction("CONTROL SETTINGS","Open original touch-control editor",()->openControls()); addAction("LAUNCHER SETTINGS","RKB preferences",()->{page="Settings";render();}); }
    private void renderInstances(){
        cardTitle("MINECRAFT INSTANCES","Create separate Minecraft installations and choose which one to play.");
        addAction("+ CREATE INSTANCE / DOWNLOAD VERSION","Choose Vanilla, Fabric, Forge, Quilt, NeoForge or a modpack",()->Tools.swapFragment(requireActivity(),net.kdt.pojavlaunch.fragments.ProfileTypeSelectFragment.class,net.kdt.pojavlaunch.fragments.ProfileTypeSelectFragment.TAG,null));
        try {
            net.kdt.pojavlaunch.instances.Instances installed = Instances.loadDisplay();
            addText("YOUR INSTANCES",11,BLUE,true);
            if(installed.list.isEmpty()) addText("No instances found yet. Create one to download a Minecraft version.",12,MUTED,false);
            for (net.kdt.pojavlaunch.instances.DisplayInstance item : installed.list) {
                String label = (item.name == null || item.name.trim().isEmpty()) ? "Minecraft Instance" : item.name;
                String version = item.versionId == null ? "Version not set" : item.versionId;
                final net.kdt.pojavlaunch.instances.DisplayInstance selected = item;
                String selectedMark = installed.selectedIndex >= 0 && installed.list.get(installed.selectedIndex) == item ? "  •  SELECTED" : "";
                addAction(label + selectedMark, "Minecraft " + version + " — tap to select this instance", () -> {
                    Instances.setSelectedInstance(selected);
                    toast("Selected: " + label + " (" + version + ")");
                    render();
                });
            }
        } catch (Exception e) { addText("Could not load instance list. Return to the launcher and try again.",12,MUTED,false); }
        addText("Tip: Each instance can keep its own Minecraft version, loader and compatible mods.",12,MUTED,false);
    }
    private void renderDownload(){
        cardTitle("DOWNLOAD CENTER","Browse community content for your Minecraft setup.");
        addText("CONTENT DOWNLOADS",11,BLUE,true);
        addAction("MODS","Find gameplay, utility, performance and PvP mods",()->openUrl("https://modrinth.com/mods"));
        addAction("SHADERS","Browse shader packs for compatible Minecraft versions",()->openUrl("https://modrinth.com/shaders"));
        addAction("RESOURCE / TEXTURE PACKS","Download visual and texture packs",()->openUrl("https://modrinth.com/resourcepacks"));
        addAction("MODPACKS","Browse ready-made mod collections",()->Tools.swapFragment(requireActivity(),net.kdt.pojavlaunch.fragments.SearchModFragment.class,net.kdt.pojavlaunch.fragments.SearchModFragment.TAG,null));
        addAction("CURSEFORGE MODS & MODPACKS","Browse additional community content",()->openUrl("https://www.curseforge.com/minecraft"));
        addText("Choose a Minecraft instance separately in the Instances tab. Mod, shader and resource-pack browsing opens the provider website; automatic downloading and importing into an instance is not yet implemented.",12,MUTED,false);
    }
    private void renderMods(){ cardTitle("MOD MANAGER","Manage .jar mods for the selected Minecraft instance."); File dir=modsDir(); if(dir==null){addText("Select a Minecraft instance first.",14,MUTED,false);return;} if(!dir.exists())dir.mkdirs(); File[] files=dir.listFiles((d,n)->n.toLowerCase().endsWith(".jar")||n.toLowerCase().endsWith(".jar.disabled")); if(files==null||files.length==0){addText("No .jar mods found in",13,MUTED,false);addText(dir.getAbsolutePath(),11,MUTED,false);return;} Arrays.sort(files,Comparator.comparing(File::getName,String.CASE_INSENSITIVE_ORDER)); for(File f:files){boolean disabled=f.getName().toLowerCase().endsWith(".jar.disabled"); addAction((disabled?"OFF  ·  ":"ON  ·  ")+f.getName(),disabled?"Tap to enable this mod":"Tap to disable this mod",()->toggleMod(f));} }
    private void toggleMod(File file){String n=file.getName(); File target=n.toLowerCase().endsWith(".jar.disabled")?new File(file.getParentFile(),n.substring(0,n.length()-9)):new File(file.getParentFile(),n+".disabled"); if(target.exists()){toast("Cannot change: target filename already exists");return;} if(file.renameTo(target)){toast(target.getName().endsWith(".disabled")?"Mod disabled":"Mod enabled");render();}else toast("Could not change mod file. Check storage permission.");}
    private void renderCursor(){cardTitle("CURSOR STUDIO","Mobile cursor preferences");addText("Cursor scale",13,MUTED,true);for(String value:new String[]{"Small","Default","Large"}) addAction(value,"Save cursor scale preference",()->{requireContext().getSharedPreferences("rkb_hub",0).edit().putString("cursor_scale",value).apply();toast("Cursor scale saved: "+value);});addText("These preferences are stored locally. Applying a custom cursor overlay depends on the active control renderer.",12,MUTED,false);}
    private void renderControl(){cardTitle("CONTROL","Existing Pojav touch controls");addAction("OPEN CONTROL EDITOR","Edit buttons and touch layout",this::openControls);addAction("GAMEPAD MAPPER","Open original gamepad mapper",()->Tools.swapFragment(requireActivity(),net.kdt.pojavlaunch.fragments.GamepadMapperFragment.class,net.kdt.pojavlaunch.fragments.GamepadMapperFragment.TAG,null));}
    private void renderAbout(){cardTitle("ABOUT RKB LAUNCHER","PLAY  •  EXPLORE  •  CREATE");addText("A mobile-first Minecraft Java launcher experience built on the PojavLauncher source.",14,MUTED,false);addAction("YOUTUBE","Open RKB channel",()->openUrl("https://www.youtube.com/"));addAction("DISCORD","Open Discord",()->openUrl("https://discord.com/"));addAction("PROJECT SOURCE","Open GitHub repository",()->openUrl("https://github.com/rkbgamerz4/RKB-LAUNCHERV1"));}
    private void renderSettings(){cardTitle("SETTINGS","RKB local preferences");boolean anim=requireContext().getSharedPreferences("rkb_hub",0).getBoolean("animations",true);addAction("UI ANIMATIONS: "+(anim?"ON":"OFF"),"Toggle RKB hub animation preference",()->{requireContext().getSharedPreferences("rkb_hub",0).edit().putBoolean("animations",!anim).apply();render();});addAction("RESET RKB PREFERENCES","Reset only RKB hub preferences",()->new AlertDialog.Builder(requireContext()).setTitle("Reset RKB preferences?").setMessage("This does not change Minecraft files or Pojav settings.").setNegativeButton("Cancel",null).setPositiveButton("Reset",(d,w)->{requireContext().getSharedPreferences("rkb_hub",0).edit().clear().apply();render();}).show());}
    private File modsDir(){try{Instance i=Instances.loadSelectedInstance();if(i==null)return null;return new File(i.getGameDirectory(),"mods");}catch(Exception e){return null;}}
    private void openControls(){try{startActivity(new Intent(requireContext(),CustomControlsActivity.class));}catch(Exception e){toast("Control editor unavailable");}}
    private void openUrl(String url){try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));}catch(Exception e){toast("No browser available");}}
    private void cardTitle(String title,String desc){LinearLayout c=new LinearLayout(requireContext());c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(14),dp(14),dp(14));c.setBackgroundColor(PANEL);TextView t=text(title,16,BLUE,true);c.addView(t);TextView d=text(desc,12,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(6);c.addView(d,p);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(12);body.addView(c,cp);}
    private void addText(String s,int size,int color,boolean bold){TextView t=text(s,size,color,bold);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(7);body.addView(t,p);}
    private void addAction(String title,String detail,Runnable action){LinearLayout c=new LinearLayout(requireContext());c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(13),dp(12),dp(13),dp(12));c.setBackgroundColor(PANEL);TextView t=text(title,14,WHITE,true);c.addView(t);TextView d=text(detail,11,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(4);c.addView(d,p);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(7);body.addView(c,cp);c.setOnClickListener(v->action.run());c.setFocusable(true);}
    private Button button(String s){Button b=new Button(requireContext());b.setText(s);b.setTextSize(10);b.setAllCaps(false);b.setTextColor(WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));return b;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(requireContext());t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);} private void toast(String s){Toast.makeText(requireContext(),s,Toast.LENGTH_SHORT).show();}
}
