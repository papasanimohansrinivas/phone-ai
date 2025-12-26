Here is the updated `README.md` with the license changed to **Apache 2.0**.

---

# Phone AI 🤖📱

**Enough thinking. Time for action.**

> **Note:** This is an **Alpha** release. It is experimental, buggy, and powerful. Use with curiosity.

**Phone AI** is an online Large Action Model (LAM) for Android. Unlike chatbots that just give you instructions, Phone AI actually *uses* your phone to get things done. You tell it what to do, and it navigates the UI, clicks buttons, and scrolls—just like a human would.

*([Youtube demo link!](https://youtube.com/shorts/8-Qi8jgGGuY?feature=share))*

## 📖 The Mission

I built this because my mom called me, frustrated that she couldn't withdraw gold investment money from her banking/UPI app.

Modern UIs are complex.

**Phone AI** bridges the digital divide. It gives non-tech-native users (and power users) a way to control their device using natural language. It doesn't rely on hard-coded scripts or APIs; it "sees" the screen and adapts dynamically.

## 🧠 How It Works

Phone AI runs on a loop of **Perception → Reasoning → Action**:

1. **Eyes (Perception Engine):**
* Uses **Android Accessibility Services** to parse the visible screen hierarchy.
* Filters out noise and detects interactive elements (even those without clear labels).


2. **Brain (Reasoning):**
* Sends the simplified UI tree and user goal to **Google Gemini 2.5 Pro** (via API).
* The model decides the next logical step (e.g., "Tap element #4" or "Scroll down").


3. **Hands (Execution):**
* Performs system-level gestures (Taps, Swipes).
* Injects hardware key events (e.g., Media Play/Pause) to handle "Touch Traps" where UI overlays fail.



## ✨ Features

* **Natural Language Control:** "Open YouTube and play the Dacoit trailer."
* **Dynamic Navigation:** Adapts to UI changes; doesn't break if a button moves.
* **Self-Correction:** Detects if an action failed (e.g., infinite loops) and tries a different strategy.
* **Overlay Handling:** Can interact with complex overlays that typically blind other screen readers.

## 🛠️ Getting Started

### Prerequisites

* Android Studio Ladybug (or newer)
* A physical Android device (Recommended) or Emulator (API 29+)
* **A Google Gemini API Key** ([Get one here](https://aistudio.google.com/app/apikey))

### Installation

1. **Clone the Repository:**
```bash
git clone https://github.com/papasanimohansrinivas/phone-ai.git

```


2. **Add your API Key:**
* Open `app/src/main/java/com/example/myapplication/LLMClient.kt` (or your specific API configuration file).
* Add: `GEMINI_API_KEY="your_key_here"`


3. **Build & Run:**
* Connect your phone via USB debugging.
* Run the `app` configuration in Android Studio.



### ⚠️ Important: Enabling Permissions

Since this app uses **Accessibility Services** (a high-power permission), Android has strict security gates for sideloaded apps.

**1. Bypass Play Protect:**
When installing, if you see a "Blocked by Play Protect" screen:

* Tap **"More details"** (small arrow).
* Select **"Install anyway"**.

**2. Enable Restricted Settings (Android 13+):**
After installation, you cannot immediately enable Accessibility.

* Go to **Settings > Apps > Phone AI**.
* Tap the **three dots (⋮)** in the top-right corner.
* Select **"Allow restricted settings"**.
* (Verify with your PIN/Fingerprint).

**3. Turn it On:**

* Now go to **Settings > Accessibility > Phone AI**.
* Toggle it **ON**.

## 🤝 Contributing

This project is in its infancy. I need help with:

* **Refining the Perception Engine:** Better filtering of the Accessibility Node tree.
* **Loop Logic:** Smarter detection of "Task Complete" states.
* **Local LLM Support:** Moving from Gemini API to on-device models (Gemini Nano / Llama).

**Found a bug?** Open an Issue.
**Fixed a bug?** Open a PR.

## 📄 License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at


[apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)



Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

---

**Built with ❤️ and Kotlin.**
*Inspired by the struggles of the non-tech-native generation.*
