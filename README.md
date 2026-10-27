# heroes3-combat-simulator

Ein **Auto-Solver für taktische Kämpfe aus *Heroes of Might and Magic III***.
Du wählst zwei Truppen, der Server simuliert den Kampf mit der gleichen
Logik wie das Spiel und liefert Ergebnis plus vollständiges Replay zurück.
Kein Spielerinput während des Kampfs — deterministische Engine über einen
injizierten Zufallsgenerator, gleicher Seed liefert identischen Ablauf.

Endziel sind belastbare Truppen-, Helden- und Fraktionsvergleiche per
Monte-Carlo: welche Einheit ist gold-effizient, welche Fraktion gewinnt
im Schnitt, wann zahlt sich welche Spezial-Synergie aus.

## Was drin ist

- **Kreaturenkatalog** — alle 126 Einheiten der neun Fraktionen (Castle,
  Rampart, Tower, Inferno, Necropolis, Dungeon, Stronghold, Fortress,
  Conflux) inklusive Upgrade-Varianten, Stats aus dem RoE-Manual, dazu die
  15 neutralen Kreaturen (Peasant und die aus Armageddon's Blade, Werte von
  heroes.thelazy.net, Fähigkeiten wie in VCMI, siehe unten).
- **Neutrale Kreaturen** — Halfling (schießt, unter einem Helden Glück
  mindestens +1), Rogue, Boar, Mummy (untot, 25 % Chance auf Curse),
  Nomad, Sharpshooter (keine Distanz- und Mauerstrafe), Troll
  (Regeneration), Gold und Diamond Golem (Zauberschaden −85 / −95 %),
  Enchanter (schießt, keine Mauer- und Nahkampfstrafe, in der ersten und
  jeder dritten Runde ein zufälliger Massenzauber auf Expert — Haste,
  Bless, Stone Skin, Cure, Bloodlust, Slow, Weakness, Air Shield —, ohne
  die Aktion zu verbrauchen), Faerie Dragon (greift bis zu fünfmal mit
  einem zufälligen Angriffszauber an, Advanced mit Power 5 je Drache;
  ist das Ziel immun, im Nahkampf; dauerhaft Magic Mirror auf Basic),
  Rust Dragon (Atem; Säure: −3 Defense je Biss, bis Kampfende und
  stapelbar, 20 % Chance auf 25 Schaden je Drache), Crystal Dragon (20 %
  Magieresistenz) und Azure Dragon (Atem, immun gegen Zauber der Stufen
  1–3, Furcht: ein lebender Gegner setzt zu Beginn seines Zugs mit 10 %
  aus). Neutrale Drachen zählen für Slayer als Drachen. Nicht im Kampf
  wirken Spionage (Rogue), Kristallerzeugung (Crystal Dragon) und Sand
  ohne Malus (Nomad, kommt mit dem Gelände).
- **Engine-evaluierte Spezialfähigkeiten** unter anderem No Retaliation,
  Two Blows / Two Shots, Good Morale (Minotaur), Good Army Morale
  (Angel/Arch Angel), Death Stare, Thunderbolts,
  Petryfying (Medusa/Scorpicore), Cursing (Black/Dread Knight 20 %, auf
  Advanced: 80 % des Mindestschadens minus 1), Poisonous, Diseases, Aging,
  Death Blow, Hass (Angel/Devil, Titan/Black Dragon, Genie/Efreet je
  +50 %, Elementare gegen ihr Gegenelement +100 %), Impact Damage
  (Cavalier-Jousting, wirkungslos gegen Pikeniere und Hellebardiere),
  Defense Reduction (Behemoth), Move Back (Harpy), Fesselung (Dendroiden:
  das Ziel bewegt sich nicht mehr, bis sie sich bewegen oder fallen),
  Mana-Entzug (Wraith: 2 je Stack und Runde) und Mana-Kanal (Familiar:
  20 % des gegnerischen Manas), Schutzzauber der Storm-, Ice-, Energy- und
  Magma-Elementare gegen ihr Element, Counterstrike
  Twice/Unlimited (Griffin), Regeneration (Wight), Fire Shield (Efreet
  Sultan), Rebirth (Phoenix), No Hand-to-Hand Penalty, Moral-Senkung
  (Bone/Ghost Dragon), Resurrection (Arch Angel: einmal pro Kampf 100 HP
  je Arch Angel für einen verbündeten Stack, auch einen ganz gefallenen,
  wenn sein Feld frei ist), Dämonen-Beschwörung (Pit
  Lord: einmal pro Kampf aus einem ganz gefallenen verbündeten Stack,
  50 HP je Pit Lord).
- **Multi-Stack-Fähigkeiten** für die Army-Battles: Cerberus
  Three-Headed Attack (Ziel + die beiden Felder links und rechts davon,
  nur Gegner), Fire Breath (Green/Gold/Red/Black Dragon, Firebird,
  Phoenix — Ziel + Stack dahinter), Angriff auf alle benachbarten Gegner
  (Hydra, Chaos Hydra, Psychic und Magic Elemental), Magog-Fireball (Ziel
  + alle 1-Hex-Nachbarn, Manual S. 97), Lich Death Cloud (Ziel + alle
  1-Hex-Nachbarn außer Untoten). In der Faction-Matrix legt Conflux damit
  zu (Greedy 0.65 → 0.77, Strategic 0.87 → 0.94).
- **Zweifeldrige Kreaturen** — 45 Kreaturen (Drachen, Behemoths,
  Cavaliers, Unicorns, Hydren und andere, Liste wie in VCMI) sowie
  Ballista, Sanitätszelt und Katapult belegen zwei Felder; das zweite
  liegt hinter dem Kopf, zum eigenen Rand hin. Nachbarschaft, Distanz,
  Splash und Flächenzauber rechnen mit beiden Feldern; Landefelder und
  am Boden jeder Schritt des Wegs brauchen Platz für beide, am Rand rückt
  die Kreatur ein Feld vor. Mit
  dem Greedy-Solver ändert das keine Fraktion in der Faction-Matrix um
  mehr als 0.05, mit dem Strategic-Solver um höchstens 0.06 (80 Kämpfe je
  Paarung). Der alte Strategic-Solver sprang hier um bis zu 0.32
  (Stronghold 0.32 → 0.64, Rampart 0.35 → 0.15); das kam von Tank-Pattern
  und Focus-Fire, die mit zweifeldrigen Kreaturen Schaden anrichteten
  (siehe unten).
- **Helden** — alle 144 Helden aus RoE und Armageddon's Blade (18
  Klassen, je Fraktion 16) mit ihren Startfertigkeiten; die Primärwerte
  hängen an der Klasse. Im Army-Battle führt ohne Auswahl der
  Standardheld der Fraktion, die den größten Goldwert der Armee stellt
  (Sorsha, Jenova, Neela, Nymus, Tamika, Damacon, Crag Hack, Tazar,
  Fiur); „ohne Helden“ lässt eine Armee führerlos kämpfen (siehe unten). Angriffs- und Verteidigungswert des Helden werden
  auf jede Kreatur addiert (Manual S. 33). Im Kampf wirken die
  Sekundärfertigkeiten Offense (+10/20/30 % Nahkampfschaden), Archery
  (+10/25/50 % Fernkampf), Armorer (−5/10/15 % erlittener Schaden),
  Leadership (+1/2/3 Moral), Luck (+1/2/3 Glück), Tactics, Artillery,
  Ballistics und First Aid (siehe unten) sowie Wisdom, die vier
  Magieschulen, Sorcery, Intelligence und Resistance (siehe Heldenzauber).
  Die übrigen (Logistics, Pathfinding, Scouting, Navigation, Estates,
  Diplomacy, Learning, Mysticism, Scholar, Eagle Eye, Necromancy) wirken
  auf der Abenteuerkarte oder nach dem Kampf und sind hier ohne Effekt.
  Je Armee einstellbar ist, was der Held unterwegs gesammelt hat:
  Level (1..99), zusätzliche Attack-/Defense-Punkte, gelernte oder
  verbesserte Fertigkeiten (höchstens acht), Glück und Moral aus
  Gebäuden und Abenteuer-Orten, dazu Power, Knowledge, das Zauberbuch
  (siehe Heldenzauber) und die Artefakte (siehe unten). Single Battle und
  Einheiten-Matrix bleiben ohne Helden.
- **Armeen ohne Helden** — neutrale Monster und Garnisonen. Ohne Helden
  gibt es kein Glück (in SoD wirkt es nur unter einem Helden,
  heroes.thelazy.net, *Luck*), keine Zauber, kein Tactics, keine
  Kriegsmaschinen und keine gesammelten Boni; die Moral aus der
  Zusammenstellung wirkt. Eine Stadt greift nur ein Held an: Belagerungen
  brauchen einen angreifenden Helden, verteidigen darf die Garnison
  allein.
- **Zufälliger Aufstieg**: Statt den Helden von Hand auszubauen, lässt er
  sich je Armee zufällig auf das gewählte Level bringen, reproduzierbar
  über den Seed. Jeder Aufstieg zieht den Primärwert nach den
  Klassen-Wahrscheinlichkeiten (Level 2–9 und ab 10 getrennt) und bietet
  zwei Fertigkeiten an: links eine vorhandene zum Verbessern (oder eine
  neue, wenn keine mehr verbesserbar ist), rechts eine aus der anderen
  Gruppe, gewichtet nach Klasse; Wisdom kommt spätestens alle 3 Level
  (Magie-Klassen) bzw. 6, eine Magieschule alle 3 bzw. 4. Gewählt wird
  immer links — so wie das Spiel Helden mit Start-Erfahrung auflevelt
  (heroes.thelazy.net). Helden mit Zauberbuch bekommen die Gilde ihrer
  Fraktion: Stufe 1 sofort, Stufe 2 ab Level 2, Stufe 3–5, sobald Wisdom
  sie öffnet, höchstens bis zur Gildenstufe der Stadt (Castle 4,
  Stronghold und Fortress 3); Tower hat die Library und lehrt je Stufe
  einen Zauber mehr. Die Zauber jeder Stufe werden wie im Spiel
  gewichtet ausgelost, Abenteuerzauber (Fly, Town Portal …) eingeschlossen —
  sie stehen im Buch, wirken im Kampf aber nicht. Might-Helden ohne Start-Zauber
  bleiben ohne Buch. Das Ergebnis zeigt die Auswertung je Seite an.
- **Spezialfähigkeiten** der Helden in der SoD-Fassung: Kreatur-
  Spezialisten geben ihren Kreaturen (Basis- und Ausbaustufe) +1 Speed und
  +5 % Attack und Defense je angefangene Spanne von Kreaturstufe Leveln;
  Fertigkeits-Spezialisten heben Offense, Archery oder Armorer um 5 % je
  Level; die Planeswalker geben ihren Elementaren feste Boni auf Attack,
  Defense und Schaden; Ballista-Spezialisten heben deren Attack.
  Helden der magischen Klassen beginnen mit Stufe 1 der Magiergilde ihrer
  Fraktion: den fünf wahrscheinlichsten Kampfzaubern der Stufe 1 (Tower mit
  Library sechs) nach den
  Gilden-Wahrscheinlichkeiten von heroes.thelazy.net (im Spiel zufällig;
  Necropolis etwa ohne Bless, mit Curse). Gegen denselben Helden ohne
  Zauber gewinnt das Buch im Spiegel-Duell 0.810 (Tower und Castle 1.000,
  Conflux 0.475). Die Standardhelden sind Kämpfer und zaubern nicht.
  Zauber-Spezialisten beginnen mit ihrem Zauber, auch über dem Wisdom-Gate:
  Schadenszauber, Cure, Animate Dead und Resurrection wirken um 3 % ×
  Level / Stufe des Ziels stärker (Ciele fest +50 % auf Magic Arrow);
  Haste, Prayer, Weakness, Precision, Stone Skin und Bloodlust geben +3/2/1
  Punkte für Kreaturen der Stufe 1–2/3–4/5–6, Slayer +4 bis +1 für Stufe
  1–4, Disrupting Ray +2, Fortune immer +3 Glück, Bless zusätzlich 3 % ×
  Level / Stufe Schaden (SoD-Werte von heroes.thelazy.net). Hypnotize,
  Fire Wall und die Ressourcen-Spezialitäten werden angezeigt, wirken aber
  nicht.
- **Heldenzauber** (Manual S. 33–39, Spell Reference S. 64–79) — ein Held
  mit Zauberbuch wirkt einmal pro Runde einen Zauber, sobald einer seiner
  Stacks am Zug ist, ohne dass der Stack seine Aktion verliert. Mana: 10
  je Knowledge, Intelligence +25/50/100 %; Spell Power = Power, sie setzt
  Schaden und Dauer (eine Runde je Punkt). Wisdom öffnet Stufe 3/4/5,
  ohne Wisdom bis Stufe 2. Die Schul-Fertigkeiten (Air, Earth, Fire,
  Water Magic) heben die Stufe, auf der gezaubert wird; auf Expert treffen
  die Massenzauber alle Stacks einer Seite. Sorcery gibt +5/10/15 %
  Zauberschaden, Resistance 5/10/20 % Chance, dass ein feindlicher Zauber
  an den eigenen Truppen abprallt. Schaden nach den Formeln des Manuals,
  Fireball, Meteor Shower, Inferno und Frost Ring als Flächen, Chain
  Lightning springt zum jeweils nächsten Stack mit halbem Schaden, Death
  Ripple, Destroy Undead und Armageddon treffen ganze Gruppen; Flächen
  treffen auch eigene Stacks. Dazu Stärkungen und Schwächungen (Bless,
  Curse, Haste, Slow, Prayer, Blind, Disrupting Ray, Forgetfulness …),
  Cure, Animate Dead und Resurrection — die beiden letzten holen auch ganz
  gefallene Stacks zurück, wenn ihr Feld frei ist, frisch und ohne
  Zauber. Golems nehmen halben bzw. ein
  Viertel Schaden, Elementare doppelten oder keinen von den Zaubern ihrer
  Gegen-Elemente; Anti-Magic sperrt die unteren Stufen. Dispel hebt alle
  Zauber auf, auf Basic nur auf eigenen Stacks, auf Advanced auf jedem,
  auf Expert auf allen. Magic Mirror lenkt feindliche Zauber auf ein
  einzelnes Ziel mit 20/30/40 % auf einen zufälligen Stack des Zaubernden
  um. Summon Elementals ruft Power × 2/3/4 Elementare als neuen Stack an
  den eigenen Rand (eine Art je Kampf, dieselbe Art erneut verstärkt ihn);
  er handelt noch in derselben Runde, sobald die Zugreihenfolge ihn
  erreicht (wie in VCMI), und zählt am Ende nicht als
  Überlebender. Bleiben einer Seite nur Beschworene, verliert sie — haben
  beide nur noch Beschworene oder gar nichts, verlieren beide. Die Art
  wählt der Held nach Goldwert abzüglich dessen, was der gegnerische Held
  ihr mit seinen Zaubern nehmen kann: meist Earth, gegen Meteor Shower Air,
  gegen Ice Bolt Water. Schnelle Elementare zu bevorzugen, verlor in der
  Messung gegen „immer Earth" deutlich. Teleport setzt den Stack am Zug
  neben den gefährlichsten Gegner, wenn er zu Fuß diese Runde keinen
  erreicht und dort nicht in die Reichweite von Nahkämpfern mit halb so
  viel Schaden wie seine Lebenskraft landet — Basic nicht über Mauern oder
  Graben, Advanced über den Graben, Expert überallhin; er kostet je nach
  Water Magic 15/12/6/3. Auf freiem Feld bringt er nichts (0.496), in der
  Belagerung mit Expert Water Magic hebt er den doppelt so starken
  Angreifer gegen eine Castle von 0.722 auf 0.908. Welchen Zauber der Held wählt, entscheidet eine
  Heuristik: erwartete vernichtete Kosten bei Schaden, geschätzter Anteil
  am Wert des Stacks bei Wirkungen, bei Schutz vor einer Schule und
  Anti-Magic der Schaden, den der gegnerische Held mit seinen Zaubern
  anrichten könnte. Liegende Zauber und Beschwörungen zählen höchstens
  zwei Runden; im Spiegel-Duell schlägt das die frühere Annahme von vier
  Runden auf allen gemessenen Leveln mit 0.52–0.56. Bei knappem Mana zählt, wie oft sich ein Zauber in
  den restlichen Runden noch bezahlen lässt — zweimal Magic Arrow schlägt
  dann einmal Lightning Bolt. Ohne Kampfwirkung und deshalb nie gewählt
  sind nur die Abenteuer-Zauber.
- **Kontroll- und Feldzauber** (heroes.thelazy.net; Reichweiten, Formen und
  Dauer wie in VCMI): Clone (Kopie eines eigenen Stacks der Stufe 1–5/6/7,
  eine Runde je Spell Power, vergeht beim ersten Schaden oder mit dem
  Original), Sacrifice (ein eigener Stack wird ohne Leiche geopfert, ein
  anderer bekommt (Power + Lebenspunkte des Geopferten + 3/6/10) × Anzahl
  Lebenspunkte zurück), Berserk (ein Stack, auf Advanced/Expert alle im
  Radius 1/2, auch eigene; der nächste Zug geht gegen den nächsten Stack,
  gleich welcher Seite), Hypnotize (ein gegnerischer Stack unter
  10/20/50 + 25 × Power vollen Lebenspunkten kämpft eine Runde je Power für
  den Zaubernden), Fire Wall (2/3 Felder, verbrennt mit 10/20/50 + 10 ×
  Power, wer hindurchgeht oder darin stehen bleibt, zwei Runden), Force
  Field (2/3 unpassierbare Felder, zwei Runden), Quicksand (4/6/8
  zufällige Felder, halten Bodentruppen an, für den Gegner unsichtbar),
  Land Mine (4/6/8 Felder, 25/50/100 + 10 × Power Schaden für Gegner, nicht
  gegen einen Gegner auf seinem Heimatgelände) und Remove Obstacle (ein
  Hindernis, ab Advanced auch Fire Wall, auf Expert alle Zauber-Felder;
  Klippen bleiben). Expert Dispel räumt alle Zauber-Felder ab. Die Wahl
  setzt Wände auf den Weg gegnerischer Bodentruppen und rechnet mit der
  Chance, dass sie in Treibsand oder Minen treten. Gemessen (ein Held mit
  Expert Wisdom und allen Schulen auf Expert, Power 7, nur dieser eine
  Zauber, gegen dieselbe Armee ohne Zauber, je Fraktion im Spiegel, 216
  Kämpfe): Clone 0.95, Berserk 0.94, Hypnotize 0.89, Land Mine 0.89, Fire
  Wall 0.78, Sacrifice 0.60; Force Field, Quicksand und Remove Obstacle um
  0.5 — auf offenem Feld kostet der Umweg selten eine Runde. Die Mana-Kosten folgen dem RoE-Manual; wahlweise
  gelten die Kosten aus Shadow of Death, nach denen ein Zauber billiger
  wird, sobald der Held seine Schule auf irgendeiner Stufe beherrscht
  (etwa Haste 5 statt 6, Fireball 12 statt 15); Magi und
  Arch Magi senken die Kosten der eigenen Zauber um 2, Pegasi heben die
  des Gegners um 2.
- **Artefakte** (heroes.thelazy.net, *List of artifacts*; Wirkungen nach VCMI
  `config/artifacts.json`): die 94 kampfwirksamen aus RoE, AB und SoD, je
  Held auf die 14 Plätze des Heldenfensters gelegt. Jedes Artefakt passt
  nur auf einen Platz seiner Art, ein Kombi-Artefakt sperrt die Plätze
  seiner übrigen Teile und führt deren Summe plus die eigene Wirkung;
  jedes Artefakt höchstens einmal. Primärwerte kommen auf den Helden
  (Attack und Defense nicht unter 0), dazu Glück und Moral (Pendant of
  Courage +3/+3), Speed (Cape of Velocity +2), Lebenspunkte (Ringe und
  Vial; Elixir of Life +4, für Lebewesen 25 % der Grund-Lebenspunkte und
  Regeneration), Zauberdauer (Ring of the Magi +56), Magieresistenz zur
  Resistance-Fertigkeit, Archery-Prozente (nur mit der Fertigkeit), Orbs
  (+50 % Schaden ihrer Schule, additiv zu Sorcery), Tomes und Spellbinder's
  Hat (alle Zauber einer Schule bzw. der Stufe 5, ohne Wisdom), Armageddon's
  Blade (Armageddon auf Expert, Armee immun dagegen), Titan's Thunder
  (Titan's Lightning Bolt, 600 Schaden ohne Mana), Pendants und Power of
  the Dragon Father (Immunitäten der Armee, auch gegen eigene Zauber),
  Badge of Courage (in SoD immun gegen Gedankenzauber), Sphere of
  Permanence (kein Dispel), Golden Bow und Bow of the Sharpshooter (keine
  Entfernungs- und Hindernisstrafe, mit dem Bogen schießen Schützen auch
  neben Gegnern), Vial of Dragon Blood (+5/+5 für Drachen) und Angelic
  Alliance (gute und neutrale Städte zählen für die Moral als eine).
  Beide Seiten treffen Recanter's Cloak (kein Held über Stufe 2), Orb of
  Inhibition (kein Held, fast keine Kreatur zaubert — Faerie Dragons
  schon), Hourglass of the Evil Hour und Spirit of Oppression (kein
  positives Glück bzw. keine positive Moral) und Orb of Vulnerability
  (natürliche Immunitäten und Magieresistenz zählen nicht — für Zauber des
  Besitzers bei allen, für Zauber des Gegners nur bei den Truppen des
  Besitzers). Angelic Alliance und Armor of the Damned wirken ihre Zauber
  (Prayer 10 Runden auf die eigenen, Slow, Curse, Weakness und Misfortune
  50 Runden auf die feindlichen Stacks, Expert, ohne Mana), sobald die
  erste eigene Kreatur am Zug ist. Was nur auf der Abenteuerkarte oder
  nach der Schlacht wirkt (Bewegung, Ressourcen, Necromancy, Eagle Eye,
  Rückzug), fehlt. Im Konfigurator gibt es eine Auswahlliste je Platz, die
  nur Passendes anbietet, und eine Zufallsauswahl nach Anzahl oder
  Goldwert. Gemessen (Helden auf Level 10 zufällig aufgestiegen, je
  Fraktion im Spiegel mit getauschten Rollen, 108 Kämpfe je Artefakt):
  Armor of the Damned, Angelic Alliance, Power of the Dragon Father und
  Titan's Thunder 1.00, Elixir of Life 0.94, Shield of the Damned 0.84,
  Sword of Hellfire 0.83, Tome of Fire Magic 0.83, Pendant of Courage 0.72,
  Cape of Velocity 0.68, Bow of the Sharpshooter 0.65; Orb of Inhibition
  und Spirit of Oppression wirken auf beide Seiten und bleiben im Spiegel
  bei 0.50.
- **Kreatur-Zauber** (Werte aus der Spell Reference des Manuals) —
  Zauber liegen mit Stufe und Rundenzahl auf dem Stack und laufen am
  Rundenende ab. Ogre Magi wirken Bloodlust, Master Genies einen
  zufälligen nützlichen Zauber (Bless, Bloodlust, Cure, Haste, Shield,
  Stone Skin, Fortune, Precision, Air Shield, Mirth, Counterstrike, Fire
  Shield, Frenzy, Prayer, Slayer), beide dreimal pro Kampf auf Advanced
  mit Spell Power 6, und zwar wenn sie in diesem Zug nicht angreifen
  können. Unicorns blenden mit 20 % Chance (Blind bis zum nächsten
  Angriff, der Gegenschlag darauf mit halbem Attack), Serpent und Dragon
  Flies heben nützliche Zauber des Ziels auf, Dragon Flies wirken dazu
  Weakness.
- **Immunität und Magieresistenz** — Black Dragon und Magic Elemental
  gegen alle Zauber, Gold und Red Dragon gegen die unteren Stufen,
  Efreet, Fire/Energy Elemental, Firebird und Phoenix gegen Feuer, Untote,
  Elementare, Golems, Gargoyles, Giants und Titans gegen Gedankenzauber,
  Troglodytes gegen Blind. Dwarves wehren feindliche Zauber zu 20/40 % ab,
  die Aura eines Unicorns gibt Nachbarn 20 %.
- **Kriegsmaschinen** (Manual S. 117) — je Armee wählbar: Die Ballista
  schießt am Rundenende auf den lohnendsten Gegner, Grundschaden (2–3) ×
  (Attack des Helden + 1) mit Attack 10 plus der Attack des Helden; alle
  Maschinen bekommen Attack und Defense ihres Helden. Artillery gibt ab
  Advanced einen zweiten Schuss und 50/75/100 % Chance auf doppelten
  Grundschaden, Ballista-Spezialisten heben ihren Attack. Bless und Curse
  setzen den Grundschaden der Ballista auf das Maximum bzw. Minimum,
  Precision und Prayer heben ihre Attack, Slayer gegen seine Ziele um 8,
  Fortune und Misfortune ändern das Glück ihrer Schüsse; der Held legt
  sie auf die eigene bzw. Curse und Misfortune auf die gegnerische
  Ballista, wenn das mehr bringt als ein anderes Ziel (bewertet am Schaden
  der Ballista; im Duell neutral, 0.503).
  Das Sanitätszelt
  heilt am Rundenende die oberste Kreatur des am stärksten verwundeten
  Stacks um 1–25 Lebenspunkte, mit First Aid bis 50/75/100. Der
  Munitionswagen gibt den Schützen unbegrenzt Schüsse. Die Maschinen
  stehen am eigenen Rand (Reihen 1, 3 und 9, wie im Spiel), schlagen
  nicht zurück und lassen sich zerstören: Ballista und Zelt hören dann auf,
  nach dem Munitionswagen verbrauchen Schützen wieder Munition. Ein Stack,
  der in seinem Zug keine Kreatur angreift, aber eine Maschine erreicht,
  greift sie an, die teuerste zuerst. Im Spiegel-Duell mit allen drei
  Maschinen auf beiden Seiten gewinnt das gegen „Maschinen stehen lassen"
  0.559 (+4.4 σ) bei einer Wochenproduktion und 0.525 (+1.8 σ) bei vier.
  Splash (Drachenatem, Magog, Cerberus), Flächenzauber, Chain Lightning
  und Armageddon treffen Maschinen, die Todeswolke der Liches nicht;
  Magic Arrow und die Bolts dürfen auf sie zielen. Gegen alle übrigen
  Zauber sind sie immun, außer gegen die Wirkungen, die heroes.thelazy.net
  als wirksam listet (Bless, Shield, Cure und andere).
  Wirkung im Spiegel-Duell (`WarMachineImpactTest`, eine Seite
  ausgerüstet, je 1440 Kämpfe): Ballista 0.601, mit Expert Artillery
  0.840, Zelt mit Expert First Aid 0.904, alle drei 0.862.
  Das Katapult gibt es nur bei Belagerungen, siehe unten.
- **Gelände** (heroes.thelazy.net, *Terrain* und *Native terrain*;
  Boni und Hindernisse wie in VCMI) — je Army-Battle wählbar: eines der
  acht Grundgelände (Erde, Sand, Gras, Schnee, Sumpf, Ödland, Untergrund,
  Lava) und darüber ein magisches Gelände. Kreaturen auf ihrem
  Heimatgelände bekommen +1 Angriff, Verteidigung und Speed (Castle,
  Rampart und Conflux Gras, Tower Schnee, Inferno Lava, Necropolis Erde,
  Dungeon Untergrund, Stronghold Ödland, Fortress Sumpf; neutrale
  Kreaturen keins). Die Hindernisse kommen aus dem Satz des Geländes:
  5–12 Felder, mit 40 % zuerst ein großes Hindernis an fester Stelle, die
  beiden Aufstellungsspalten bleiben frei. Magische Gelände: Magic Plains
  (alle Zauber auf Expert, auch die der Kreaturen), Rocklands, Fiery
  Fields, Lucid Pools und Magic Clouds (Zauber ihrer Schule auf Expert),
  Cursed Ground (Helden nur Zauber der Stufe 1, Kreaturen gar keine, keine
  Heimatboni, keine positive Moral, kein Glück), Holy Ground und Evil Fog
  (gute bzw. böse Kreaturen +1 Moral, die anderen −1) und Clover Field
  (Stronghold, Fortress und Conflux +2 Glück). Eine Belagerung findet
  immer auf dem Heimatgelände der Stadt statt. Ohne Angabe bleibt das Feld
  wie bisher ohne Gelände: keine Heimatboni, zufällig verteilte Felsen.
  Gemessen (Strategic, Wochenproduktion mit Standardhelden, 12 Seeds je
  Paarung): auf dem eigenen Gelände gewinnen Fortress 0.23 → 0.53,
  Stronghold 0.52 → 0.68, Rampart 0.26 → 0.41, Inferno 0.20 → 0.36,
  Necropolis 0.24 → 0.34, Dungeon 0.61 → 0.71; Castle, Tower und Conflux
  liegen schon oben und gewinnen wenig dazu.
- **Seeschlacht** (VCMI `battlefields.json`) — Wasser als Gelände: Treffen
  zwei Helden in Booten aufeinander, kämpfen sie auf zwei Schiffen; die
  Spalten 6–10 dazwischen sind Wasser, nur zwei Planken (dritte und neunte
  Reihe) führen hinüber. Alle übrigen Kämpfe auf Wasser — gegen neutrale
  Wächter oder eine Garnison ohne Helden — finden auf einem Schiffsdeck
  statt, dessen Hindernisse Masten sind (drei Felder je Mast). Auf Wasser
  hat niemand Heimatgelände, magisches Gelände gibt es dort nicht. Gemessen
  (Wochenproduktionen aller Paarungen, Helden auf Level 10): auf zwei
  Schiffen dauert ein Kampf 6.7 statt 6.2 Runden.
- **Kreaturenbanken** (VCMI `config/objects/creatureBanks.json`) — die elf
  Banken aus SoD (Cyclops Stockpile, Dwarven Treasury, Griffin Conservatory,
  Imp Cache, Medusa Stores, Naga Bank, Dragon Fly Hive, Shipwreck, Derelict
  Ship, Crypt, Dragon Utopia) mit je vier Stufen. Die Stufe ist wählbar oder
  wird nach ihrem Anteil auf der Karte gewürfelt (30/30/30/10 %), ein
  Wächter-Stack ist mit 50 % die ausgebaute Kreatur (aus dem Seed). Die
  Wächter ersetzen den Verteidiger und stehen in der Aufstellung der Bank
  (VCMI-Layouts `creatureBankNarrow/Wide`): die Armee des Helden in der
  Mitte, die Wächter an den Ecken und Rändern ringsum. Keine Tactics, keine
  Hindernisse, keine Kriegsmaschinen; nur ein Held greift eine Bank an,
  eine Belagerung ist sie nicht. Gemessen (Wochenproduktion je Fraktion,
  Helden auf Level 10, 36 Kämpfe je Stufe): Imp Cache, Medusa Stores,
  Shipwreck, Derelict Ship und Crypt fallen auf allen Stufen; Dwarven
  Treasury und Dragon Fly Hive erst auf Stufe 4 nicht immer (0.61/0.75);
  Cyclops Stockpile, Griffin Conservatory und Naga Bank nur auf Stufe 1;
  Dragon Utopia nie.
- **Belagerung** (heroes.thelazy.net, *Fortifications*, Felder wie in der
  freien H3-Engine VCMI) — je Army-Battle wählbar: Der Verteidiger steht
  in einer Stadt, ohne Felsen auf dem Feld. Die Stadt ist frei wählbar;
  ohne Angabe die Fraktion seines Helden, bei einer Garnison ohne Helden
  ihre Hauptfraktion (nach Goldwert). Ohne Verteidiger-Helden schießen
  die Türme nach der KI des Spiels, Landminen machen 150 Schaden.
  Fort baut Mauern und Tor: sechs unzerstörbare Mauerstücke, vier
  Segmente mit 2 Treffern (mit Castle 3), das Tor mit 2. Mauern sperren
  Bodentruppen; Flieger fliegen darüber, und ein Schuss von draußen über
  eine stehende Mauer verliert die Hälfte (außer Magi). Die Zugbrücke
  öffnet sich nur für den Verteidiger und bleibt unten, solange ein Stack
  auf dem Tor steht — auch ein gefallener, dann kommen die Angreifer
  hindurch. Ein Stack vor dem Tor sperrt sie für beide Seiten. Citadel fügt den Hauptturm und den Graben
  hinzu, Castle zwei Seitentürme. Die Türme schießen zu Rundenbeginn,
  Schaden für eine voll ausgebaute Stadt, ohne Defense des Ziels. Der
  Graben hält Bodentruppen auf, nimmt ihnen 3 Defense und verletzt sie
  beim Betreten und zu Beginn jedes Zugs dort (70, bei Dungeon, Inferno
  und Fortress 90; Fortress zweireihig). Gibt es zum Ziel einen Weg ohne
  Graben, den der Stack in diesem Zug schafft — etwa durch das offene oder
  zerstörte Tor —, nimmt er ihn (wie in VCMI, wo Grabenfelder anhalten); Tower legt stattdessen
  Landminen gegen Angreifer. Jeder angreifende Held bringt ein Katapult
  mit, das nach den Türmen auf die Befestigung schießt — Zielwahl,
  Trefferchancen und Schaden je Ballistics-Stufe nach den SoD-Werten.
  Zyklopen schießen wie ein Katapult mit Basic Ballistics auf die Mauern,
  Cyclops Kings mit Advanced, solange noch keine Bresche offen ist; der
  Held kann Earthquake wirken (2/3/4 Treffer auf zufällige Teile). Ohne
  Artillery wählt die Spiel-KI die Ziele der Türme, samt ihrem Fehler,
  mit einem Schaden von 1 zu rechnen; mit Artillery zielt der Verteidiger
  selbst. Der Verteidiger stellt ohne Tactics auf und hat keine Ballista.
  Messung (`SiegeImpactTest`, Spiegel-Duell, Standardhelden, je 360
  Kämpfe, Win-Rate des Verteidigers): Bei gleich großen Armeen gewinnt er
  auf freiem Feld 0.552, im Fort 0.609, in der Citadel 0.861, in der Castle
  immer. Mit doppelter Armee nimmt der Angreifer ein Fort sicher ein, eine
  Citadel hält dann 0.072, eine Castle 0.278, mit Expert Ballistics 0.111;
  mit vierfacher Armee fällt jede Stadt. Der Graben kostet den Angreifer
  je Kampf ein bis vier Treffer, rund 5 % seiner Armee; jeder Weg durch
  eine Bresche endet im Graben. Die Mauerschüsse der Zyklopen bringen Stronghold mit
  doppelter Armee gegen eine Citadel 0.630 statt 0.534 (je 540 Kämpfe).
- **Tactics** (Manual S. 42) — hat ein Held die Fertigkeit auf höherer
  Stufe als der Gegner, stellt er vor Kampfbeginn in den ersten 3/5/7
  Spalten auf (ein, zwei, drei Grade Vorsprung). Die Nahkämpfer rücken in
  ihrer Reihe an die vorderste freie Zelle vor, höchstens fünf Spalten
  tief. Die Schützen rücken mit, wenn die Zone fünf Spalten erreicht —
  dann trifft ihr erster Schuss ohne Distanzstrafe; bei drei Spalten
  bleiben sie am Rand. Im Spiegel-Duell gewinnt die Tactics-Seite damit
  0.555/0.588/0.555 (Basic/Advanced/Expert, je 1440 Kämpfe), mit Schützen
  am Rand 0.555/0.528/0.530.
- **Moral** (Manual S. 43) — jede Armee hat ein Rating, das sich aus ihrer
  Zusammenstellung ergibt: +1, wenn alle Kreaturen aus derselben Stadt
  stammen (außer Necropolis), −1 für Untote unter Lebenden, −1 je
  Stadt-Typ ab dem dritten, dazu Angel/Arch Angel, die Leadership und die
  gesammelte Moral des Helden. Steht auf der Gegenseite ein Bone oder Ghost Dragon, sinkt das
  Rating um 1 (Manual S. 103), egal wie viele Drachen es sind. Positive
  Moral gibt eine Chance auf eine zweite Aktion, negative eine Chance,
  die Aktion ganz zu verlieren (4.2 / 8.3 / 12.5 %). Untote
  und Elementare stehen außerhalb — ihre Moral ist immer 0.
- **Glück** (Manual S. 44) — Armee-Rating 0..3 aus Luck-Fertigkeit und
  gesammeltem Glück des Helden; Devils und Arch Devils auf der Gegenseite
  ziehen einen Punkt ab (Manual S. 99). Wie in SoD wirkt Glück nur mit
  Held und wird nie negativ. Ein Glückstreffer (4.2 / 8.3 / 12.5 %) gibt
  +100 % auf den Grundschaden, nur gegen das angegriffene Ziel.
- **Rundenablauf in zwei Phasen** (Manual S. 43): Stacks ziehen nach
  Speed absteigend; wer *Wait* wählt, verschiebt seine Aktion ans Ende
  der Runde und handelt dort in umgekehrter Reihenfolge — langsamste
  Wartende zuerst. Jeder Stack darf pro Runde nur einmal warten. Bei
  gleichem Speed zieht zu Kampfbeginn der Angreifer (Manual S. 40),
  danach die Seite, die zuletzt nicht am Zug war.
  *Defend* gibt dafür +20 % Defense bis Rundenende. Die Auto-Solver
  nutzen das taktisch: wer sein Ziel diese Runde nicht erreicht, wartet,
  statt sich exponiert ins Niemandsland zu stellen.
- **Hex-Schlachtfeld 15 × 11** wie in H3 als Rechteck mit versetzten
  Reihen, abwechselnd 15 und 14 Hexe breit und damit exakt
  links-rechts-symmetrisch: Beide Armeen stellen sich spiegelbildlich am
  Rand auf. Zufällige Hindernisse pro Sim, Hex-A*-Pathfinding um
  Obstacles herum, zugbasierte Move-Sequenz pro Hex (Token wandert
  sichtbar Schritt für Schritt).
- **Fernkampf** mit `shotsRemaining`, Nahkampf-Penalty bei Distanz 1,
  ½-Schaden ab >10 Hex Distanz und nochmal ½, wenn der Schuss über eine
  stehende Stadtmauer geht — beide negierbar durch entsprechende Marker.
  Gewöhnliche Hindernisse halbieren nichts (heroes.thelazy.net, *Obstacle
  penalty*). Bis Oktober 2026 galt die Strafe auch für Felsen; mit der
  Korrektur gewinnen schützenlastige Armeen deutlich (Strategic, je 640
  Kämpfe: Tower 371 → 445, Dungeon 395 → 456, Rampart 196 → 227 Siege),
  Necropolis, Fortress und Stronghold verlieren 36–54 Siege.
- **HTTP-API** über Spring Boot 4 mit
  [Swagger-UI](http://localhost:8080/swagger-ui.html): `GET /api/units`,
  `GET /api/factions`, `GET /api/heroes`, `GET /api/artifacts`,
  `GET /api/creature-banks`, `POST /api/battles/simulate` für einzelne Kämpfe,
  `POST /api/army-battles/simulate` + `GET /api/army-battles/presets`
  für Army-vs-Army, `POST /api/experiments/matrix` für Matrix-
  Auswertungen. Antworten enthalten Ergebnis plus strukturierten
  `BattleEvent`-Stream (sealed Interface, Jackson-polymorph serialisiert,
  Action-Events tragen `actorSlot`/`targetSlot` für Multi-Stack-
  Disambiguierung).
- **Matrix-Auswertung** — jede Einheit gegen jede andere, pro Match-up
  mehrere Seeds und automatisch getauschte Attacker-/Defender-Rollen.
  Parallel über Work-Stealing-Pool (50 %-Auslastung der CPU-Kerne, per
  `h3.experiment.parallelism-percent` justierbar), asynchron mit
  Polling-Endpoint und Fortschrittsanzeige. Vier Stack-Sizing-Modi:
  gleiche Stack-Größe, gleiches Gold (LCM-Snap), Wochenproduktion und
  gold-normalisierte Wochenproduktion. Tier- und Faction-Filter im
  Config; Report mit Per-Unit- und Per-Faction-Tabelle plus
  „Anomalien" (Truppen, die mehrheitlich gegen die nächstniedrigere
  Tier-Klasse verlieren).
- **Army-vs-Army-Modus** — bis zu 7 Stacks pro Seite auf einer
  Spawn-Spalte (Reihen `{0, 2, 4, 5, 6, 8, 10}`), Zugreihenfolge wie im
  Rundenablauf beschrieben. Jede Armee führt ein Held (ohne Auswahl der
  Standardheld ihrer Fraktion) oder kämpft ohne Helden. Hartkodierte
  Wochenproduktions-Presets pro Faction (CASTLE, RAMPART, …, CONFLUX)
  als Default-Composition, frei überstellbar via 7-Slot-Editor.
  **Taktische Aufstellung statt Slot-Direct**: `SpawnLayout`
  positioniert Schützen nach `unit.health()` aufsteigend auf die
  äußersten Reihen (zerbrechlichster Schütze in die Ecke — weniger
  Adjazenz-Hexen, also weniger Angriffsfläche), Melees
  nach Speed absteigend von der Mitte nach außen (schnellster
  zentriert für maximale Charge-Reichweite). Schützen innen oder auf
  den Reihen 2/8 brachten im Duell nichts (0.497 / 0.511 bei 6480
  Kämpfen). Der UI-Slot bestimmt nur die Anzeige-Reihenfolge im Editor.
- **Strategischer Solver** (`StrategicAutoSolver`) für Army-Battles:
  Pro Runde wird ein `RoundPlan` mit einem Focus-Fire-Ziel je Seite
  berechnet (gewichtet nach Schaden × Count plus Special-Boni). In der
  Ziel-Wahl gehen vor: Schützen mit einem Gegner daneben schlagen den
  Nachbarn, Flieger greifen ungedeckte Schützen an, AoE-Schützen und
  Feuer-Atem suchen den besten Netto-Treffer (mit Veto gegen Eigentore),
  dann das Focus-Ziel, für Nahkämpfer nur, wenn sie es diese Runde
  erreichen; sonst entscheidet der `GreedyAutoSolver`, dessen Bewegung,
  Warten und Landefeld-Wahl der Strategic-Solver übernimmt. Ein früheres
  Tank-Pattern (Nahkämpfer decken die eigenen Schützen) verlor im direkten
  Duell gegen die Fassung ohne (0.469 bei 6480 Kämpfen) und ist entfallen.
  Gegen Greedy gewinnt der Strategic-Solver 0.580 (vorher 0.397).
  Solche Vergleiche misst der `SolverDuelTest`: zwei Varianten (Solver,
  Aufstellung oder beides) direkt gegeneinander über alle Paarungen mit
  getauschten Rollen, aktiviert mit `-Ph3.harness=solver-duel`, einzelne
  Duelle mit `-Ph3.duel=<name>`.
  Single-Battle nutzt den `GreedyAutoSolver`, die Faction-Matrix beide.
- **Replay-UI** — React 19 + Vite + Tailwind. Drei Modi über den
  ModeSwitcher: Single-Battle (Truppenkonfig + Hex-Replay mit
  Step/Pause/Speed-Slider + Rückspiel mit getauschten Seiten),
  Matrix-Auswertung (Faction-/Unit-Excludes, sortierbare Ergebnis-
  Tabelle), Army-vs-Army (Faction-Preset-Picker + 7-Slot-Editor pro
  Seite, Replay rendert bis zu 14 Tokens, Combat-Log mit Stack-
  Namen-Auflösung). Deutsch und Englisch umschaltbar.
- **Deterministisch über Seed**: gleicher Seed → identische
  `BattleResult` + identischer Event-Stream + identische Obstacle-
  Verteilung. Voraussetzung für reproduzierbare Auswertungen.

## Schnellstart

Voraussetzung: JDK 25 auf dem PATH (sonst lädt die Gradle-Toolchain
nach), Node ist nicht erforderlich — Gradle ruft den Frontend-Build
selbst auf.

```pwsh
.\gradlew.bat bootRun
```

Spring Boot lauscht auf `http://localhost:8080`, liefert die fertige
React-UI unter `/`, die REST-API unter `/api/**`, Swagger-UI unter
`/swagger-ui.html`. Beim ersten Lauf installiert Gradle die
Node-Dependencies und baut das Frontend.

CLI-Demo ohne UI (Grand Elf vs. Arch Angel mit deutschem Combat-Log auf
der Konsole):

```pwsh
.\gradlew.bat bootRun --args='--spring.profiles.active=cli'
```

## Entwickeln

```pwsh
.\gradlew.bat build              # Java-Build + Tests + Coverage-Gate + SpotBugs
.\gradlew.bat test               # nur Java-Tests
cd frontend; npm run dev         # Vite-Dev-Server auf :5173 mit /api-Proxy auf :8080
cd frontend; npm test            # Vitest + Testing Library + MSW
.\gradlew.bat dependencyUpdates  # Versions-Check
```

Backend-Hot-Reload im Dev-Modus: `ComSimApp` als Spring-Boot-Run-Config
starten, separat im Frontend-Verzeichnis `npm run dev` — Vite reicht
`/api/*` an den Spring-Server durch, keine CORS-Konfiguration nötig.

Reports landen unter `build/reports/`: JaCoCo, SpotBugs, Tests.

## Stack

- **Engine**: Java 25, Spring Boot 4, ErrorProne + NullAway, SpotBugs,
  JaCoCo (Coverage-Gate Line ≥ 80 % / Branch ≥ 60 % erzwungen).
- **API-Schicht**: Spring MVC, Validation per `jakarta.validation`,
  springdoc-openapi für OpenAPI/Swagger.
- **Frontend**: React 19, Vite 8, TypeScript 6, Tailwind 4,
  TanStack Query, Zustand, React Router 7, react-i18next.
- **Tests**: JUnit 5 + AssertJ + Spring-Boot-Test (Backend);
  Vitest + Testing Library + MSW (Frontend).
- **CI**: GitHub Actions auf `push`/`pull_request` gegen `master`,
  JDK 25 Temurin, `gradlew build spotbugsMain spotbugsTest
  jacocoTestReport`.

## Architektur

Feature-first nach Konvention `<fachlich>.<technisch>` mit
`technisch ∈ { ui | application | domain | values }` unter
`de.zettsystems.h3comsim`:

- **`battle.domain`** — pure Combat-Domäne ohne Spring-Abhängigkeit.
  `Unit`, `UnitCatalog`, `Stack` (mit `slot`-Feld), `Hex`,
  `Battlefield`, `PathFinder`, `ObstacleGenerator`, `BattleSetup`
  mit `List<Stack>` pro Seite, `Battle.simulate(...)` als
  Orchestrator, `AutoSolver`-Interface plus zwei Implementierungen
  (`GreedyAutoSolver`, `StrategicAutoSolver` mit `RoundPlan`), Helden
  und Zauber (`Hero`, `HeroCatalog`, `HeroSpellcasting`, `Spell`),
  Kriegsmaschinen (`WarMachine`, `BallistaSpells`), Belagerung
  (`Siege`, `SiegeLayout`, `Catapult`, `ArrowTowers`), Gelände und Wasser
  (`Terrain`, `MagicTerrain`, `TerrainObstacles`), Artefakte (`Artifact`,
  `Equipment`, `ArtifactSpells`) und Kreaturenbanken (`CreatureBank`).
  Event-Records unter `battle.domain.events`.
- **`singlebattle`** — Ausprägung „ein Stack gegen einen Stack".
  `BattleSimulationService` + `BattleController`
  (`POST /api/battles/simulate`).
- **`armybattle`** — Ausprägung „bis zu 7 Stacks pro Seite".
  `ArmyBattleService`, `FactionPresetCatalog`, `SpawnLayout`,
  `ArmyBattleController` (`POST /api/army-battles/simulate`,
  `GET /api/army-battles/presets`).
- **`matrix`** — Ausprägung „viele 1-vs-1" (paarweise alle gegen alle).
  Parallel via Work-Stealing-Pool, asynchrone Jobs mit Polling-Endpoint.
- **`setup` + `config`** — Cross-cutting: `CatalogController`,
  `SpaForwardingController`, CORS-Config, CLI-Runner.
- **`frontend/`** — eigenständiges Vite-Projekt, vom Gradle-Build als
  statisches Resource gebündelt. Feature-Buckets: `battle-config`,
  `battle-replay`, `matrix-experiment`, `army-config`.

## Erste Erkenntnisse aus der Einheiten-Matrix

Die Einheiten-Matrix vergleicht einzelne Stacks ohne Helden und ohne
Zauber. Vier vergleichbare Läufe gegen denselben Pool (63 Upgrade-Einheiten,
9 Fraktionen × 7 Tiers, 20 Seeds, je Match-up beide Rollen → ≈ 78 k
Sims pro Modus). Faction-Win-Rate gemittelt über alle Unit-vs-Unit-
Match-ups:

| Rang   | EQUAL_COUNT       | EQUAL_GOLD         | WEEKLY_PRODUCTION | EQUAL_GOLD_WEEKLY  |
|--------|-------------------|--------------------|-------------------|--------------------|
| 1      | Castle 52.6 %     | **Rampart 59.3 %** | **Castle 54.7 %** | **Rampart 59.2 %** |
| 2      | Dungeon 50.8 %    | Tower 58.6 %       | Rampart 51.4 %    | Tower 58.1 %       |
| 3      | Tower 50.2 %      | Castle 53.9 %      | Dungeon 51.1 %    | Castle 53.4 %      |
| 4      | Inferno 50.1 %    | Stronghold 53.0 %  | Conflux 50.6 %    | Stronghold 53.3 %  |
| 5      | Stronghold 49.7 % | Inferno 50.5 %     | Tower 50.0 %      | Inferno 50.7 %     |
| 6      | Necropolis 49.5 % | Conflux 48.1 %     | Stronghold 49.7 % | Dungeon 48.3 %     |
| 7      | Fortress 49.2 %   | Dungeon 47.4 %     | Fortress 49.3 %   | Conflux 48.0 %     |
| 8      | Rampart 49.2 %    | Fortress 43.0 %    | Necropolis 47.4 % | Fortress 43.1 %    |
| 9      | Conflux 48.7 %    | Necropolis 36.2 %  | Inferno 45.7 %    | Necropolis 35.8 %  |
| Spread | 3.9 pp            | **23.1 pp**        | 9.0 pp            | **23.4 pp**        |

**Was die Modi inhaltlich erzählen**:

- **EQUAL_COUNT** ist für Faction-Ranking unaussagekräftig — alle neun
  Factions liegen in einem 4-pp-Band um die 50 %. Jede Faction hat eine
  starke T7 (Arch Angel 99 %, Arch Devil 97 %, Black Dragon 95 %, …),
  und bei gleicher Stack-Größe mittelt sich das raus.
- **EQUAL_GOLD** spreizt drastisch (23 pp). Rampart und Tower
  führen, weil ihre Mid-Tier-Einheiten gold-effizient sind (Iron Golem
  200 g/35 HP, Naga Queen, War Unicorn, Dendroid Soldier). Necropolis
  bricht auf 36 % ein — Lich, Black Knight, Ghost Dragon sind teuer
  gemessen an ihren Stats, und ihr eigentlicher Hebel (Necromancer-
  Skill, Animate Dead) fehlt in der Engine.
- **WEEKLY_PRODUCTION** bleibt nahe an der Tier-Hierarchie (9 pp Spread,
  nur 2 Anomalien gegenüber 23 bei EQUAL_GOLD), weil die Produktionsraten
  den Tier-Gap einpreisen (14 Pikemen pro Woche, 1 Angel pro Woche).
- **EQUAL_GOLD_WEEKLY** liefert nahezu dieselbe Rangfolge wie EQUAL_GOLD
  (Top 5 identisch, max ±0.8 pp Differenz). Mathematisch erwartbar — die
  Wochengewichtung hebt sich im Budget weitgehend wieder auf. Der Modus
  ist primär eine Plausibilitätskontrolle des Gold-Vergleichs.

**Stabile Befunde**:

- **Castle** ist die robusteste Faction (Top 3 in drei von vier Modi).
- **Rampart + Tower** sind die gold-effizientesten Factions.
- **Necropolis** verliert in jeder gold-normierten Auswertung — ein
  Modell-Artefakt, weil Necromancy/Animate-Dead/Skeleton-Transformer
  nicht modelliert sind.
- **Magma Elemental** ist die einzige Anomalie über alle vier Modi
  (T5 verliert mehrheitlich gegen T4) — bekanntermaßen die schwächste
  Tier-5-Einheit, deren Fire-Immunity-Vorteil ohne Spells nicht zieht.

**Modell-Grenzen, die diese Zahlen verzerren**:

- Keine Helden und keine Heldenzauber (beides gibt es nur im
  Army-Battle) → Necropolis (Animate Dead), Tower (Magi-Casts), Conflux
  (Spell-Imm./Fire-Imm.), Black Dragon (Spell-Imm.) verlieren ihren
  H3-Hauptvorteil.
- Necromancy wirkt erst nach dem Kampf und ist nicht abgebildet.
- Im Single-Battle-Modus fehlen die Multi-Stack-AoE-Effekte (Cerberus,
  Fire-Breath, Magog Splash, Lich Death Cloud) — die werden erst im
  Army-vs-Army-Modus aktiv.

Die Werte sind also als „unter spell-/hero-freien 1-vs-1-Bedingungen"
zu lesen, nicht als universelle H3-Tier-Liste.

## Was bewusst fehlt

Jede Schlacht, die das Spiel kennt — Feld, Gelände, Belagerung, Wasser,
Kreaturenbank, mit und ohne Helden —, lässt sich abbilden.

Bewusst nicht abgebildet, weil es nichts am Kampf selbst ändert:
Rückzug und Kapitulation, die Abenteuerkarte (Bewegung, Logistics,
Pathfinding, Scouting …), alles nach dem Kampf (Necromancy, Eagle Eye,
Erfahrung), Abenteuer-Zauber und die Inhalte späterer Erweiterungen
(Kanone, Commander). Zweifeldrige Kreaturen drehen sich nicht um, wenn
sie rückwärts gehen — wie in VCMI; eine Regel, nach der sich dabei ihre
Felder änderten, ist nicht belegt. Der Verteidiger einer Stadt steht wie
im Spiel immer rechts.

## Quellen

- Stat-Werte aus dem RoE-Manual (`files/heroes3_manual.pdf`,
  Text-Layer-Extrakt unter `files/h3_manual.txt`).
- Spezialfähigkeiten: https://heroes.thelazy.net/index.php/Special_ability
