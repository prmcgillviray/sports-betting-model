import streamlit as st
import pandas as pd
import math

st.set_page_config(page_title="Airow Sports Model", layout="wide")
st.title("🏈 Airow NFL Auto-Model")

# --- STEP 1: LOAD DATA ---
@st.cache_data
def load_data():
    # This pulls live data from a public sports repository
    url = "https://raw.githubusercontent.com/fivethirtyeight/data/master/nfl-elo/nfl_elo_latest.csv"
    df = pd.read_csv(url)
    
    # Filter for the current season (latest date in file)
    current_season = df[df['date'] >= '2024-09-01']
    return current_season

# Load the data
try:
    data = load_data()
    st.success("Data Feed Active: Connected to NFL Database")
except:
    st.error("Data Feed Error: Could not reach repository")

# --- STEP 2: SELECT TEAMS ---
st.header("Matchup Selector")
# Get unique team names
teams = sorted(data['team1'].unique())

col1, col2 = st.columns(2)
with col1:
    home_team = st.selectbox("Home Team", teams, index=teams.index("KC") if "KC" in teams else 0)
with col2:
    away_team = st.selectbox("Away Team", teams, index=teams.index("BUF") if "BUF" in teams else 1)

# --- STEP 3: GET TEAM STATS (ELO) ---
# We grab the most recent rating for each team from the database
home_stats = data[data['team1'] == home_team].iloc[-1]
away_stats = data[data['team1'] == away_team].iloc[-1]

h_rating = home_stats['elo1_pre']
a_rating = away_stats['elo1_pre']

st.write(f"**{home_team} Rating:** {h_rating:.0f} | **{away_team} Rating:** {a_rating:.0f}")

# --- STEP 4: THE MATH ---
# Standard Elo Formula
diff = h_rating - a_rating + 50 # Adding 50 points for Home Field Advantage
win_prob = 1 / (1 + 10 ** (-diff / 400))

# --- STEP 5: DISPLAY ---
st.divider()
st.metric(label=f"{home_team} Win Probability", value=f"{win_prob:.1%}")

if win_prob > 0.65:
    st.success(f"Model Recommendation: HAMMER {home_team}")
elif win_prob < 0.35:
    st.error(f"Model Recommendation: HAMMER {away_team}")
else:
    st.warning("Model Recommendation: STAY AWAY (Too Close)")
