#!/usr/bin/env python3
"""Summarize real simulation CSVs from sharded deployments, not fabricated feedback."""
import argparse, csv, json, statistics, collections
from pathlib import Path

def summarize(rows):
    n=len(rows)
    if not n: return {"sessions":0}
    wins=sum(r['victory']=='true' for r in rows)
    shops=[int(r['first_coins']) for r in rows if int(r['first_coins'])>=0]
    buyers=[int(r['first_buys']) for r in rows if int(r['first_buys'])>=0]
    stages=[int(r['stage']) for r in rows]
    return dict(sessions=n,profiles=len({r['profile'] for r in rows}),wins=wins,
        win_pct=round(100*wins/n,2),mean_stage=round(statistics.mean(stages),2),median_stage=statistics.median(stages),
        reached_30=sum(s>=30 for s in stages),reached_50=sum(s>=50 for s in stages),
        time_capped_or_left=sum(r['ended']=='false' for r in rows),
        median_first_coins=statistics.median(shops) if shops else None,median_first_buys=statistics.median(buyers) if buyers else None,
        median_combat_seconds=statistics.median(int(r['seconds']) for r in rows))

def group(rows,key):
    out=collections.defaultdict(list)
    for r in rows: out[r[key]].append(r)
    return {k:summarize(v) for k,v in sorted(out.items())}

def main():
    p=argparse.ArgumentParser();p.add_argument('root',type=Path);p.add_argument('--expected-profiles',type=int,default=1000);p.add_argument('--days',type=int,default=30);p.add_argument('--out',type=Path,default=Path('cohort-summary.json'));a=p.parse_args()
    result={}
    for version in ('alpha7','alpha8'):
        files=sorted(a.root.glob(f'cohort-{version}-*/sessions.csv'))
        rows=[];keys=set();duplicate_count=0
        for f in files:
            with f.open() as stream:
                for r in csv.DictReader(stream):
                    key=(r['profile'],r['day'],r['session'])
                    if key in keys: duplicate_count+=1;continue
                    keys.add(key);rows.append(r)
        days=collections.defaultdict(set)
        for r in rows: days[int(r['profile'])].add(int(r['day']))
        expected_ids=set(range(a.expected_profiles));full=sum(days[i]==set(range(1,a.days+1)) for i in expected_ids)
        result[version]={'shards':len(files),'duplicates':duplicate_count,'complete_profile_days':full,'expected_profiles':a.expected_profiles,'archetypes':len({r['archetype'] for r in rows}),'gender_labels':dict(collections.Counter(r['gender'] for r in rows)), 'overall':summarize(rows),'difficulty':group(rows,'difficulty'),'preferred_branch':group(rows,'preferred_branch'),'first_evolution':group([r for r in rows if r['first_evolution']!='None'],'first_evolution'),'final_evolution':group(rows,'final_evolution'),'motive':group(rows,'motive'),'day':group(rows,'day'),'team':group(rows,'team')}
    a.out.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({v:{'overall':d['overall'],'complete_profile_days':d['complete_profile_days'],'shards':d['shards']} for v,d in result.items()},indent=2))
if __name__=='__main__':main()
