# AI Test Case Generator

A simple Java tool that takes any website URL, fetches its HTML content, and
uses Google's Gemini AI to automatically generate a structured list of QA
test cases plus a bug report — all in one console run.

## How it works

1. The program fetches the raw HTML of a given URL (`HttpClient`, no external
   libraries needed for this part).
2. It builds a prompt asking Gemini to analyze the page and produce:
   - Numbered, concrete test cases (functional, UI, edge cases) with steps
     and expected results (`TC001`, `TC002`, ...)
   - A bug report listing real issues found in the code (`BUG01`, `BUG02`,
     ...), each with a description, expected behavior, and severity
     (Low/Medium/High)
3. The JSON response from the Gemini API is parsed manually (no JSON
   library) to extract just the generated text, which is printed cleanly to
   the console.

## Tech stack

- **Java** (`java.net.http.HttpClient`) — for both fetching the target
  website and calling the AI API. No external dependencies.
- **Maven** — project structure and build (`pom.xml`).
- **Google Gemini API** (`gemini-3.6-flash`) — free tier, no credit card
  required. (An earlier version of this project used the Anthropic Claude
  API, but was switched to Gemini for free access.)

## Setup

1. Get a free API key at [aistudio.google.com/apikey](https://aistudio.google.com/apikey).
2. Set it as an environment variable:
   ```
   GEMINI_API_KEY=your_key_here
   ```
   In IntelliJ: **Run → Edit Configurations → Environment variables**.
3. Run the program. By default it tests
   `https://www.spacejam.com/1996/` if no URL argument is passed:
   ```
   mvn compile exec:java -Dexec.args="https://example.com"
   ```
   or pass no argument to use the default URL.

## Why this project

Built as a small experiment to see how AI can speed up the first pass of QA
work — quickly mapping out what needs to be tested and surfacing obvious
issues in a page's code. It doesn't replace a QA engineer's judgment, but it
cuts down the time spent on the initial groundwork.

## Example run — real output

Below is the actual, unedited output from running the tool against
`https://www.spacejam.com/1996/`.

```
Testing site: https://www.spacejam.com/1996/
=== Test cases for https://www.spacejam.com/1996/ ===

TEST CASE LIST

TC001 - Homepage load and display
Steps: 1) Open a web browser. 2) Enter the URL https://www.spacejam.com/1996/
in the address bar and press Enter. 3) Check the visual display of the page.
Expected result: The page loads successfully, the tab title is "Space Jam",
the background shows a starry night sky, and all central elements and icons
are visible.

TC002 - Main navigation menu functionality
Steps: 1) Open the homepage. 2) Click each icon in the main menu arranged
around the central logo (Press Box Shuttle, Jam Central, Planet B-Ball,
Lunar Tunes, The Lineup, Jump Station, Junior Jam, Stellar Souvenirs, Site
Map, Behind the Jam). 3) After each click, verify the redirect and return
to the homepage.
Expected result: Every icon in the main menu successfully leads to its
corresponding internal page or frame, with no 404 errors.

TC003 - Redirect to external store (Warner Studio Store)
Steps: 1) Open the homepage. 2) Locate the "Warner Studio Store" icon.
3) Click on the icon.
Expected result: The external store site (shop.looneytunes.com) opens in a
new browser tab with the correct UTM parameters in the URL.

TC004 - Navigation via footer links
Steps: 1) Scroll to the bottom of the homepage. 2) Click, in order, the
links "Privacy Policy", "Terms", "Accessibility", and "AdChoices".
Expected result: Each footer link successfully opens the corresponding
Warner Bros. legal page in a new browser tab.

TC005 - Page display with images disabled (edge case / accessibility)
Steps: 1) Disable image loading in the browser settings. 2) Open the
homepage. 3) Check whether alt text is visible.
Expected result: Correct alt text is shown in place of images (e.g.
"Space Jam", "Press Box Shuttle", "Lunar Tunes"), allowing understandable
navigation for the user.

TC006 - Responsiveness and display across screen resolutions (UI/layout
edge case)
Steps: 1) Open the homepage on a desktop browser. 2) Shrink the browser
width to mobile dimensions (e.g. 360px or 768px) or open the site on a
mobile device. 3) Check the layout of elements.
Expected result: The table-based layout stays structured, but a horizontal
scrollbar appears without breaking the basic design.

TC007 - Loading external scripts and analytics services
Steps: 1) Open the homepage with Developer Tools (F12) open on the Console
and Network tabs. 2) Check that scripts from policies.warnerbros.com and
googletagmanager.com load successfully.
Expected result: External JS scripts for the privacy policy and Google
Analytics (gtag.js) load successfully with no blocking errors in the
console.


BUG REPORT

BUG01 - Mixed security protocol (HTTP instead of HTTPS) in the footer
Description: The "Terms" and "Accessibility" footer links use the insecure
HTTP protocol (http://policies.warnerbros.com/...), while "Privacy Policy"
and "AdChoices" use secure HTTPS.
Expected behavior: All external links should consistently use HTTPS for
security and to avoid warnings in modern browsers.
Severity: Medium

BUG02 - Use of outdated and non-standard HTML tags and attributes
Description: The page code uses deprecated HTML tags (<center>, <font>,
<nobr>) and styling attributes on the <body> tag (bgcolor, background,
text, link, vlink, alink).
Expected behavior: Text, background, and alignment styling should be
handled exclusively via CSS in line with modern web standards (HTML5).
Severity: Low

BUG03 - Leftover commented-out code in production
Description: The HTML source contains commented-out blocks of code
including old links and inactive scripts (e.g. a commented-out video link
with fastbreak.gif, and a commented-out Omniture JS function
trackOmnitureScode).
Expected behavior: Unused and inactive code should be removed from the
production codebase to improve maintainability and reduce file size.
Severity: Low

BUG04 - Reliance on unprocessed SSI (Server Side Includes) directives
Description: Ad blocks and buttons in the HTML rely on Server Side Includes
directives in the form of HTML comments (e.g. <!--#include virtual="..."-->).
If the server is not configured to process SSI, these sections remain
empty and produce empty tables in the layout.
Expected behavior: Ads and dynamic content should be loaded via modern
JavaScript or properly processed server-side templates.
Severity: Medium
```
