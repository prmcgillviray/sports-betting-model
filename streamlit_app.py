import streamlit as st
import pandas as pd

st.set_page_config(page_title="Airow Sports Model", layout="wide")
st.title("🏈 Airow NFL Command Center")

# --- STEP 1: INTERNAL DATA FEED ---
# Since public feeds can break, we host our own 'Power Ratings' here.
# (Rating 1500 is average. 1700+ is Elite. 1300 is Poor.)
data = {
    'Team': ['KC (Chiefs)', 'DET (Lions)', 'BUF (Bills)', 'BAL (Ravens)', 
             'PHI (Eagles)', 'MIN (Vikings)', 'PIT (Steelers)', 'WAS (Commanders)',
             'GB (Packers)', 'SF (49ers)', 'HOU (Texans)', 'ATL (Falcons)'],
    'Rating': [1760, 1745, 1720, 1715, 
               1690, 1640, 1630, 1590, 
               1585, 1580, 1550, 1510]
}
df = pd.DataFrame(data)

st.success("✅ Internal Database Loaded: 2024-25 Power Ratings")

# --- STEP 2: THE DASHBOARD ---
col1, col2, col3 = st.columns([1, 1, 2])

with col1:
    st.subheader("Home Team")
    home_team = st.selectbox("Select Home", df['Team'], index=0)
    # Look up rating
    h_rating = df.loc[df['Team'] == home_team, 'Rating'].values[0]
    st.info(f"Power Rating: {h_rating}")

with col2:
    st.subheader("Away Team")
    away_team = st.selectbox("Select Away", df['Team'], index=2)
    # Look up rating
    a_rating = df.loc[df['Team'] == away_team, 'Rating'].values[0]
    st.info(f"Power Rating: {a_rating}")

# --- STEP 3: THE VARIABLES ---
with col3:
    st.subheader("Model Settings")
    hfa = st.slider("Home Field Advantage (Points)", 0, 100, 55)
    
# --- STEP 4: THE CALCULATION ---
# Elo Formula: We calculate the 'Spread' and 'Win Prob'
diff = h_rating - a_rating + hfa
win_prob = 1 / (1 + 10 ** (-diff / 400))
implied_spread = diff / 25  # Rough rule of thumb: 25 Elo points = 1 point spread

# --- STEP 5: THE OUTPUT ---
st.divider()
st.metric(label=f"Win Probability for {home_team}", value=f"{win_prob:.1%}")

if win_prob > 0.60:
    st.success(f"💰 STRATEGY: Bet {home_team} to Win")
    st.write(f"Estimated Spread: {home_team} by {implied_spread:.1f} points")
elif win_prob < 0.40:
    st.error(f"💰 STRATEGY: Bet {away_team} to Win")
    st.write(f"Estimated Spread: {away_team} by {abs(implied_spread):.1f} points")
else:
    st.warning("⚠️ STRATEGY: Stay Away (Game is too close)")
