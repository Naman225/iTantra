// iTantra Interactive Web Experience
(function() {
  'use strict';

  // --- 1. TACTICAL TRANSCEIVER HUD CONTROLLER ---
  const hudModes = [
    {
      name: "ROUTINE TRANSCEIVER",
      tier: "GREEN · COMM ACTIVE",
      color: "#10B981",
      seq: "0482",
      lang: "Hindi (हिन्दी)",
      text: "राहत दल सेक्टर 4 ग्रिड की ओर रवाना हो चुका है।",
      bytes: "48 Bytes",
      bitrate: "132 bps",
      saving: "99.92%",
      flags: "0x00 (Routine PTT)",
      crc: "0x7A1F"
    },
    {
      name: "TACTICAL ALERT",
      tier: "AMBER · HAZARD DETECTED",
      color: "#F59E0B",
      seq: "0483",
      lang: "English (en-IN)",
      text: "Warning: High flood surge breach observed at sector 2 embankment.",
      bytes: "72 Bytes",
      bitrate: "164 bps",
      saving: "99.88%",
      flags: "0x40 (Alert Priority)",
      crc: "0x9E4B"
    },
    {
      name: "SOS LIFE-SAFETY DISTRESS",
      tier: "RED · SOS DISTRESS",
      color: "#EF4444",
      seq: "0484",
      lang: "Tamil (தமிழ்)",
      text: "SOS: அவசர மருத்துவ உதவி தேவை, மூன்று பேர் சிக்கியுள்ளனர்.",
      bytes: "62 Bytes",
      bitrate: "141 bps",
      saving: "99.90%",
      flags: "0x80 (SOS Distress Override)",
      crc: "0xC23A"
    }
  ];

  let currentHudIdx = 0;
  let hudInterval = null;

  const pillEl = document.getElementById('hudLivePill');
  const dotEl = document.getElementById('hudDot');
  const seqEl = document.getElementById('hudSeq');
  const langEl = document.getElementById('hudLang');
  const textEl = document.getElementById('hudText');
  const bytesEl = document.getElementById('hudBytes');
  const flagsEl = document.getElementById('hudFlags');
  const crcEl = document.getElementById('hudCrc');
  const statBytesEl = document.getElementById('hudStatBytes');
  const statBitrateEl = document.getElementById('hudStatBitrate');
  const statSavingEl = document.getElementById('hudStatSaving');
  const hudBtns = document.querySelectorAll('.hud-btn');

  function updateHud(idx) {
    currentHudIdx = idx;
    const mode = hudModes[idx];

    if (pillEl) {
      pillEl.textContent = mode.tier;
      pillEl.style.borderColor = mode.color;
      pillEl.style.color = mode.color;
    }
    if (dotEl) {
      dotEl.style.background = mode.color;
    }
    if (seqEl) seqEl.textContent = mode.seq;
    if (langEl) langEl.textContent = mode.lang;
    if (textEl) textEl.textContent = `"${mode.text}"`;
    if (bytesEl) bytesEl.textContent = mode.bytes;
    if (flagsEl) flagsEl.textContent = mode.flags;
    if (crcEl) crcEl.textContent = mode.crc;

    if (statBytesEl) statBytesEl.textContent = mode.bytes;
    if (statBitrateEl) statBitrateEl.textContent = mode.bitrate;
    if (statSavingEl) statSavingEl.textContent = mode.saving;

    hudBtns.forEach((b, i) => {
      if (i === idx) {
        b.classList.add('active');
        b.style.borderColor = mode.color;
        b.style.color = mode.color;
      } else {
        b.classList.remove('active');
        b.style.borderColor = '';
        b.style.color = '';
      }
    });

    animateSpectrum(idx);
  }

  function animateSpectrum(idx) {
    const bars = document.querySelectorAll('.spectrum-bar');
    const color = hudModes[idx].color;
    bars.forEach((b) => {
      const h = Math.floor(Math.random() * 40) + 12;
      b.style.height = `${h}px`;
      b.style.background = color;
    });
  }

  // Set up spectrum bars
  const spectrumContainer = document.getElementById('hudSpectrum');
  if (spectrumContainer) {
    for (let i = 0; i < 24; i++) {
      const bar = document.createElement('div');
      bar.className = 'spectrum-bar';
      bar.style.height = '14px';
      spectrumContainer.appendChild(bar);
    }
  }

  hudBtns.forEach((btn, idx) => {
    btn.addEventListener('click', () => {
      clearInterval(hudInterval);
      updateHud(idx);
    });
  });

  updateHud(0);
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (!reducedMotion) {
    hudInterval = setInterval(() => {
      const nextIdx = (currentHudIdx + 1) % hudModes.length;
      updateHud(nextIdx);
    }, 4000);

    setInterval(() => {
      animateSpectrum(currentHudIdx);
    }, 600);
  }

  // --- 2. INTERACTIVE PACKET SIZE & BANDWIDTH CALCULATOR ---
  const calcInput = document.getElementById('calcInput');
  const pcmValEl = document.getElementById('calcPcmVal');
  const opusValEl = document.getElementById('calcOpusVal');
  const itantraValEl = document.getElementById('calcItantraVal');
  const savingValEl = document.getElementById('calcSavingVal');
  const loraTimeEl = document.getElementById('calcLoraTime');
  const pcmBar = document.getElementById('calcPcmBar');
  const opusBar = document.getElementById('calcOpusBar');
  const itantraBar = document.getElementById('calcItantraBar');
  const presetPills = document.querySelectorAll('.preset-pill');

  function calculate(text) {
    if (!text || text.trim() === '') {
      text = "Emergency rescue underway.";
    }

    // Encoder UTF-8 byte length
    const encoder = new TextEncoder();
    const payloadBytes = encoder.encode(text).length;
    // iTantra binary frame: 1B Magic + 1B Flags + 2B SeqNum + 2B Length + Payload + 2B CRC16 = 8B overhead
    const totalFrameBytes = 8 + payloadBytes;

    // Estimate spoken audio duration: ~3.5 syllables/sec or ~14 chars/sec
    const charCount = text.length;
    const estDurationSec = Math.max(1.8, Math.min(8.0, charCount / 12.0));

    // Raw 16kHz 16-bit Mono PCM: 16000 * 2 = 32,000 bytes/sec
    const rawPcmBytes = Math.round(estDurationSec * 32000);

    // Standard VoIP Opus codec at 24 kbps: 3,000 bytes/sec
    const opusBytes = Math.round(estDurationSec * 3000);

    // Bandwidth savings percentage
    const savings = ((1 - (totalFrameBytes / rawPcmBytes)) * 100).toFixed(2);

    // LoRa SX1262 transmission time @ 125 kHz BW, SF7 (effective payload rate ~5400 bps = 675 B/s)
    const loraTimeSec = (totalFrameBytes / 675).toFixed(2);

    if (pcmValEl) pcmValEl.textContent = `${rawPcmBytes.toLocaleString()} B (${(rawPcmBytes / 1024).toFixed(1)} KB)`;
    if (opusValEl) opusValEl.textContent = `${opusBytes.toLocaleString()} B (${(opusBytes / 1024).toFixed(1)} KB)`;
    if (itantraValEl) itantraValEl.textContent = `${totalFrameBytes} Bytes (8B Header + ${payloadBytes}B Payload)`;
    if (savingValEl) savingValEl.textContent = `${savings}% Bandwidth Saved`;
    if (loraTimeEl) loraTimeEl.textContent = `~${loraTimeSec}s over LoRa radio (Instantaneous)`;

    // Update relative comparison bars
    if (pcmBar) pcmBar.style.width = '100%';
    if (opusBar) opusBar.style.width = `${Math.min(95, Math.max(12, (opusBytes / rawPcmBytes) * 100))}%`;
    if (itantraBar) {
      const pct = Math.max(2.5, Math.min(10, (totalFrameBytes / rawPcmBytes) * 100 * 8));
      itantraBar.style.width = `${pct}%`;
      itantraBar.textContent = `${totalFrameBytes} B`;
    }
  }

  if (calcInput) {
    calcInput.addEventListener('input', (e) => {
      calculate(e.target.value);
    });

    presetPills.forEach((pill) => {
      pill.addEventListener('click', () => {
        presetPills.forEach(p => p.classList.remove('active'));
        pill.classList.add('active');
        const phrase = pill.getAttribute('data-phrase');
        calcInput.value = phrase;
        calculate(phrase);
      });
    });

    calculate(calcInput.value);
  }

})();
