(function(){'use strict';
var m=document.getElementById('menu'),n=document.getElementById('nav');
m.addEventListener('click',function(){var o=n.classList.toggle('open');m.setAttribute('aria-expanded',o)});
n.addEventListener('click',function(e){if(e.target.tagName==='A')n.classList.remove('open')});
// Airtime for a 5-second voice message: raw 64 kbps audio (40,000 B) vs one 48-byte TantraPacket
var s=document.getElementById('link'),r=document.getElementById('raw'),t=document.getElementById('tan'),rb=document.getElementById('rawbar'),tb=document.getElementById('tanbar');
function fmt(x){return x<1?x.toFixed(2)+' s':x<120?x.toFixed(1)+' s':(x/60).toFixed(1)+' min'}
function calc(){var bps=+s.value,a=40000*8/bps,b=48*8/bps;r.textContent=fmt(a);t.textContent=fmt(b);rb.style.width='100%';tb.style.width=Math.max(b/a*100,1)+'%'}
s.addEventListener('change',calc);calc();
})();
