// iTantra Tactical Radio Bus Telemetry HUD
(function () {
  'use strict';

  const needle = document.getElementById('needle');
  const valEl = document.getElementById('riskVal');
  const labEl = document.getElementById('riskLab');
  const pillEl = document.getElementById('tierPill');

  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // Scenarios representing tactical RF operating states
  const scenarios = [
    { val: '48 B', lab: 'FRAME PAYLOAD', angle: 180, label: 'GREEN · 99.9% BANDWIDTH SAVED', color: '#33C481' },
    { val: '132 bps', lab: 'EFFECTIVE BITRATE', angle: 130, label: 'GREEN · 100% OFFLINE MESH', color: '#33C481' },
    { val: '0.18s', lab: 'LORA AIR TIME', angle: 70, label: 'AMBER · TACTICAL ALERT PRIORITY', color: '#E3A008' },
    { val: 'SOS', lab: 'TACTICAL DISTRESS', angle: 0, label: 'RED · CONTINUOUS VIBRATION ACTIVE', color: '#E5484D' },
  ];

  let idx = 0;

  function paint() {
    const s = scenarios[idx];
    if (needle) {
      needle.setAttribute('transform', `rotate(${s.angle} 240 240)`);
      needle.querySelectorAll('line, circle').forEach(el => {
        if (el.tagName === 'line') el.setAttribute('stroke', s.color);
        if (el.tagName === 'circle') el.setAttribute('fill', s.color);
      });
    }
    if (valEl) {
      valEl.textContent = s.val;
      valEl.style.color = s.color;
    }
    if (labEl) {
      labEl.textContent = s.lab;
    }
    if (pillEl) {
      pillEl.textContent = s.label;
      pillEl.style.borderColor = s.color;
      pillEl.style.color = s.color;
    }
  }

  paint();
  if (!reduced) {
    setInterval(() => {
      idx = (idx + 1) % scenarios.length;
      paint();
    }, 3400);
  }
})();
