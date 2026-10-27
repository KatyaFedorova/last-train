# Nova Bank (demo)

A clickable mobile banking demo in one page: cards, transfers, IBAN
payments, currency exchange, credits, savings jars, a market and a
support chat. All money and data are made up and reset on reload.

## On your phone, like a real app

Once this is merged to `main`, GitHub Pages serves it at
<https://katyafedorova.github.io/last-train/bank/>.

- **iPhone (Safari):** open the link, tap Share, then **Add to Home Screen**.
- **Android (Chrome):** open the link, tap ⋮, then **Install app** or **Add to Home screen**.

It then opens from its own icon, full screen, without the browser bar,
and works offline.

## Locally

Open `bank-app/index.html` in a browser at phone width, or serve it:
`python3 -m http.server -d bank-app 8000`.

Edit the demo data at the top of the `<script>` in `index.html`.
