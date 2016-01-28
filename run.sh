ant rebuild

XMPPdomain=boven-cute.dev.local
XMPPhost=boven-cute.dev.local
MUCdomain=conference.boven-cute.dev.local
ProxyXMPPdomain=boven-cuty.dev.local
ProxyXMPPhost=boven-cuty.dev.local
ProxyMUCdomain=conference.boven-cuty.dev.local

Lusers=1
Rusers=1

#./jitsi-proxy.sh -XMPPdomain $XMPPdomain -XMPPhost $XMPPhost -MUCdomain $MUCdomain -ProxyXMPPdomain $ProxyXMPPdomain -ProxyXMPPhost $ProxyXMPPhost -ProxyMUCdomain $ProxyMUCdomain -Lusers 1 -Rusers 2 -room $1 -ivf resources/big-buck-bunny_trailer_track1_eng.ivf
./jitsi-proxy.sh -XMPPdomain $XMPPdomain -XMPPhost $XMPPhost -MUCdomain $MUCdomain -ProxyXMPPdomain $ProxyXMPPdomain -ProxyXMPPhost $ProxyXMPPhost -ProxyMUCdomain $ProxyMUCdomain -Lusers 1 -Rusers 2 -room $1 -ivf resources/big-buck-bunny_trailer_track1_eng.ivf > Proxy.log 2>&1

