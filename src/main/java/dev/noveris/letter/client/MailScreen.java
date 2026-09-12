package dev.noveris.letter.client;

import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.network.MailSnapshotPayload;
import dev.noveris.letter.network.RequestMailSnapshotPayload;
import dev.noveris.letter.network.SelectCourierPayload;
import dev.noveris.letter.network.SendLetterPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

public final class MailScreen extends Screen {
    private static final int BG=0xEA0D0C09, PANEL=0xFF17140E, FIELD=0xFF080807, GOLD=0xFFFFD84D, ACTIVE=0xFFD6A800, TEXT=0xFFFFFBEB, MUTED=0xFFC9BE9B;
    private static final String[] TABS={"RECEBIDAS","ENVIADAS","ESCREVER","CARTEIRO"};
    private static final List<net.minecraft.resources.ResourceLocation> COURIERS=List.of(CourierAppearanceRegistry.MOSSBLOOM_ID,CourierAppearanceRegistry.COATI_ID,CourierAppearanceRegistry.RED_PANDA_ID,CourierAppearanceRegistry.BOOPLET_ID,CourierAppearanceRegistry.CAPYBARA_ID);
    private static final String[] NAMES={"MOSSBLOOM","COATI","PANDA-VERMELHO","BOOPLET","CAPIVARA"};
    private int tab,page,totalPages=1,selectedCourierIndex;
    private boolean anonymous;
    private EditBox recipient,subject,message;
    private List<MailSnapshotPayload.Entry> entries=List.of();
    private List<String> onlinePlayers=List.of();
    public MailScreen(int tab){super(Component.literal("Serviço Postal de Noveris"));this.tab=Math.max(0,Math.min(3,tab));}
    @Override protected void init(){if(tab==2)createCompose();refreshPlayers();if(tab<2)requestPage();}
    private void createCompose(){int x=left()+24,w=right()-x-24;recipient=addRenderableWidget(new EditBox(font,x,top()+105,w,24,Component.literal("Destinatário")));recipient.setMaxLength(80);recipient.setHint(Component.literal("Digite o nick..."));subject=addRenderableWidget(new EditBox(font,x,top()+145,w,24,Component.literal("Assunto")));subject.setMaxLength(120);message=addRenderableWidget(new EditBox(font,x,top()+185,w,70,Component.literal("Mensagem")));message.setMaxLength(1500);}
    private void refreshPlayers(){if(Minecraft.getInstance().getConnection()==null){onlinePlayers=List.of();return;}List<String> n=new ArrayList<>();String self=Minecraft.getInstance().player==null?"":Minecraft.getInstance().player.getGameProfile().getName();for(PlayerInfo p:Minecraft.getInstance().getConnection().getOnlinePlayers())if(!p.getProfile().getName().equals(self))n.add(p.getProfile().getName());onlinePlayers=n;}
    private void requestPage(){PacketDistributor.sendToServer(new RequestMailSnapshotPayload(tab,page));}
    public void setSnapshot(MailSnapshotPayload p){if(p.tab()==tab){page=p.page();totalPages=Math.max(1,p.totalPages());entries=p.entries();}}
    private void selectTab(int i){tab=i;page=0;clearWidgets();init();}
    private void send(){if(recipient==null||message==null||recipient.getValue().isBlank()||message.getValue().isBlank())return;PacketDistributor.sendToServer(new SendLetterPayload(recipient.getValue().trim(),subject.getValue().trim(),message.getValue(),anonymous));onClose();}
    @Override public boolean mouseClicked(double mx,double my,int button){if(super.mouseClicked(mx,my,button))return true;int l=left(),r=right(),ty=top()+44,tw=(r-l-36)/4;for(int i=0;i<4;i++){int x=l+12+i*(tw+4);if(in(x,ty,tw,28,mx,my)){selectTab(i);return true;}}
        if(tab==3){int y=top()+100,gap=8,cw=(r-l-48-gap*4)/5;for(int i=0;i<5;i++){int x=l+24+i*(cw+gap);if(in(x,y,cw,bottom()-y-50,mx,my)){selectedCourierIndex=i;PacketDistributor.sendToServer(new SelectCourierPayload(COURIERS.get(i)));return true;}}return false;}
        if(tab==2){if(in(l+24,bottom()-42,100,28,mx,my)){onClose();return true;}if(in(l+136,bottom()-42,120,28,mx,my)){anonymous=!anonymous;return true;}if(in(r-130,bottom()-42,106,28,mx,my)){send();return true;}}
        if(tab<2){int y=top()+100;for(MailSnapshotPayload.Entry e:entries){if(in(l+24,y,r-l-48,34,mx,my))return true;y+=40;}if(in(l+24,bottom()-42,110,28,mx,my)&&page>0){page--;requestPage();return true;}if(in(r-134,bottom()-42,110,28,mx,my)&&page+1<totalPages){page++;requestPage();return true;}}
        return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){int l=left(),t=top(),r=right(),b=bottom();g.fill(0,0,width,height,0x55000000);g.fill(l,t,r,b,BG);g.fill(l+3,t+3,r-3,b-3,PANEL);g.fill(l+3,t+3,r-3,t+6,GOLD);g.drawString(font,"SERVIÇO POSTAL DE NOVERIS",l+18,t+15,GOLD,false);g.drawString(font,"Correspondência segura, entregue pelo serviço postal.",l+20,t+29,MUTED,false);int tw=(r-l-36)/4;for(int i=0;i<4;i++)button(g,l+12+i*(tw+4),t+44,tw,28,TABS[i],i==tab,mx,my);if(tab==2)drawCompose(g,l,r,b);else if(tab==3)drawCouriers(g,l,r,t,b,mx,my);else drawLetters(g,l,r,b);super.render(g,mx,my,pt);}
    private void drawCompose(GuiGraphics g,int l,int r,int b){g.drawString(font,"ESCREVER CORRESPONDÊNCIA",l+24,top()+82,GOLD,false);g.drawString(font,"Jogadores online: "+onlinePlayers.size(),l+24,top()+272,MUTED,false);button(g,l+24,b-42,100,28,"CANCELAR",false,0,0);button(g,l+136,b-42,120,28,anonymous?"ANÔNIMA ✓":"ANÔNIMA",anonymous,0,0);button(g,r-130,b-42,106,28,"ENVIAR",true,0,0);}
    private void drawCouriers(GuiGraphics g,int l,int r,int t,int b,int mx,int my){g.drawString(font,"CARTEIROS",l+24,t+78,GOLD,false);g.drawString(font,"Modelos e animações importados do Clutter: Bestiary.",l+24,t+91,MUTED,false);int y=t+105,gap=8,cw=(r-l-48-gap*4)/5;for(int i=0;i<5;i++){int x=l+24+i*(cw+gap);boolean s=i==selectedCourierIndex,h=in(x,y,cw,b-y-50,mx,my);g.fill(x,y,x+cw,b-50,h?0xFF2A2417:FIELD);g.fill(x,y,x+cw,y+3,s?GOLD:0xFF5C4C22);g.drawCenteredString(font,NAMES[i],x+cw/2,y+26,s?GOLD:TEXT);g.drawCenteredString(font,"VOANDO",x+cw/2,y+48,MUTED);button(g,x+8,b-84,cw-16,28,s?"SELECIONADO":"SELECIONAR",s,mx,my);}}
    private void drawLetters(GuiGraphics g,int l,int r,int b){g.drawString(font,tab==0?"CORRESPONDÊNCIAS RECEBIDAS":"CORRESPONDÊNCIAS ENVIADAS",l+24,top()+82,GOLD,false);int y=top()+105;for(MailSnapshotPayload.Entry e:entries){g.fill(l+24,y,r-24,y+34,FIELD);g.drawString(font,e.sender(),l+34,y+5,TEXT,false);g.drawString(font,e.subject(),l+34,y+18,MUTED,false);y+=40;}g.drawString(font,"Página "+(page+1)+" / "+totalPages,l+24,b-30,MUTED,false);button(g,l+24,b-42,110,28,"ANTERIOR",false,0,0);button(g,r-134,b-42,110,28,"PRÓXIMA",false,0,0);}
    private void button(GuiGraphics g,int x,int y,int w,int h,String text,boolean active,int mx,int my){g.fill(x,y,x+w,y+h,active?ACTIVE:0xFF5C512F);g.drawCenteredString(font,text,x+w/2,y+10,active?0xFF0B0904:TEXT);}
    private boolean in(int x,int y,int w,int h,double mx,double my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    private int left(){return Math.max(12,(width-760)/2);}private int right(){return Math.min(width-12,left()+760);}private int top(){return Math.max(10,(height-420)/2);}private int bottom(){return Math.min(height-10,top()+420);}
}
